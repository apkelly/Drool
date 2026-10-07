package com.github.apkelly.drool.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import kotlinx.io.IOException
import com.github.apkelly.drool.data.local.DroolDatabase
import com.github.apkelly.drool.data.local.inTransaction
import com.github.apkelly.drool.data.local.entity.CacheMetadataEntity
import com.github.apkelly.drool.data.local.entity.TeamRelationshipEntity
import com.github.apkelly.drool.data.local.entity.TeamHubCacheEntity
import com.github.apkelly.drool.data.local.entity.ClubRelationshipEntity
import com.github.apkelly.drool.data.mapper.toEntity
import com.github.apkelly.drool.data.mapper.toLinkedEntity
import com.github.apkelly.drool.data.mapper.toDomain
import com.github.apkelly.drool.data.mapper.mergeEmergencyContacts
import com.github.apkelly.drool.data.remote.DriblHttpException
import com.github.apkelly.drool.data.remote.DriblResponseException
import com.github.apkelly.drool.data.remote.SportsRemoteDataSource
import com.github.apkelly.drool.data.storage.BearerTokenStore
import com.github.apkelly.drool.data.time.TimeProvider
import com.github.apkelly.drool.domain.model.CachedData
import com.github.apkelly.drool.domain.model.Club
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FixtureStatus
import com.github.apkelly.drool.domain.model.FamilyProfile
import com.github.apkelly.drool.domain.model.FamilyClub
import com.github.apkelly.drool.domain.model.FamilyTeam
import com.github.apkelly.drool.domain.model.RefreshFailure
import com.github.apkelly.drool.domain.model.RefreshResult
import com.github.apkelly.drool.domain.model.Team
import com.github.apkelly.drool.domain.model.TeamRelationship
import com.github.apkelly.drool.domain.model.TeamHub
import com.github.apkelly.drool.domain.model.primaryProfileId
import com.github.apkelly.drool.domain.repository.SportsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SportsRepositoryImpl(
    private val api: SportsRemoteDataSource,
    private val database: DroolDatabase,
    private val store: BearerTokenStore,
    private val timeProvider: TimeProvider,
) : SportsRepository {
    override fun observeClubs(): Flow<CachedData<List<Club>>> =
        combine(
            database.clubDao().observeAll(),
            database.cacheMetadataDao().observe(CLUBS_KEY, PUBLIC_SCOPE),
        ) { clubs, metadata ->
            CachedData(
                value = clubs.map { it.toDomain() },
                lastUpdatedEpochMillis = metadata?.updatedAtEpochMillis,
                isStale = metadata.isStale(CLUBS_TTL),
            )
        }

    override fun observeTeams(clubId: String?): Flow<CachedData<List<Team>>> {
            val source = if (clubId == null) {
                database.teamDao().observeAll()
            } else {
                database.teamDao().observeByClub(clubId)
            }
            return combine(
                source,
                database.cacheMetadataDao().observe(
                    TEAMS_KEY,
                    clubId ?: PUBLIC_SCOPE,
                ),
        ) { teams, metadata ->
            CachedData(
                value = teams.map { it.toDomain() },
                lastUpdatedEpochMillis = metadata?.updatedAtEpochMillis,
                isStale = metadata.isStale(TEAMS_TTL),
            )
        }
    }

    override fun observeFixtures(): Flow<CachedData<List<Fixture>>> =
        database.profileDao().observeActive()
            .map { it?.accountId }
            .flatMapAccountFixtures()

    override fun observeFamilyFixtures(profileIds: Set<String>): Flow<List<Fixture>> =
        database.fixtureDao()
            .observeForAccounts(profileIds.toList())
            .map { fixtures ->
                fixtures.map { it.toDomain() }
                    .upcomingAt(timeProvider.nowEpochMillis())
            }

    override fun observeFamilyTeams(profileIds: Set<String>): Flow<List<FamilyTeam>> =
        combine(
            database.teamDao().observeAll(),
            database.teamDao().observeRelationships(profileIds.toList()),
        ) { teams, relationships ->
            val teamsById = teams.associateBy { it.id }
            relationships.mapNotNull { relationship ->
                teamsById[relationship.teamId]?.let { team ->
                    FamilyTeam(
                        profileId = relationship.accountId,
                        team = team.toDomain(),
                        relationship = relationshipFrom(relationship.relationship),
                    )
                }
            }
        }

    override fun observeFamilyClubs(profileIds: Set<String>): Flow<List<FamilyClub>> =
        combine(
            database.clubDao().observeAll(),
            database.clubDao().observeRelationships(profileIds.toList()),
        ) { clubs, relationships ->
            val clubsById = clubs.associateBy { it.id }
            relationships.mapNotNull { relationship ->
                clubsById[relationship.clubId]?.let { club ->
                    FamilyClub(relationship.accountId, club.toDomain())
                }
            }
        }

    override fun observeTeamRelationships(): Flow<Map<String, TeamRelationship>> =
        database.profileDao().observeActive()
            .map { it?.accountId }
            .flatMapRelationships()

    override suspend fun refreshClubs(force: Boolean): RefreshResult =
        refreshPublic(CLUBS_KEY, CLUBS_TTL, force, database.clubDao().count()) { token ->
            val clubs = api.fetchClubs(token)
            database.inTransaction {
                database.clubDao().deleteAll()
                database.clubDao().upsertAll(clubs.map { it.toEntity() })
                markUpdated(CLUBS_KEY, PUBLIC_SCOPE)
            }
        }

    override suspend fun refreshTeams(
        force: Boolean,
        clubId: String?,
    ): RefreshResult {
        val scope = clubId ?: PUBLIC_SCOPE
        val existingCount = clubId?.let { database.teamDao().countForClub(it) }
            ?: database.teamDao().count()
        return refreshPublic(TEAMS_KEY, TEAMS_TTL, force, existingCount, scope) { token ->
            val teams = api.fetchTeams(token, clubId)
            database.inTransaction {
                if (clubId != null) {
                    database.teamDao().deleteUnrelatedForClub(clubId)
                }
                database.teamDao().upsertAll(teams.map { it.toEntity() })
                markUpdated(TEAMS_KEY, scope)
            }
        }
    }

    override suspend fun refreshFixtures(force: Boolean): RefreshResult {
        val token = store.getBearerToken()
            ?: return RefreshResult.Failed(RefreshFailure.Unauthorized, hasCachedData = false)
        val accountId = store.getActiveAccountId()
            ?: return RefreshResult.Failed(RefreshFailure.Unauthorized, hasCachedData = false)
        val existingCount = database.fixtureDao().countForAccount(accountId)
        val metadata = database.cacheMetadataDao().get(FIXTURES_KEY, accountId)
        if (!force && !metadata.isStale(FIXTURES_TTL)) return RefreshResult.NotModified

        return runRefresh(existingCount > 0) {
            val fixtures = (
                api.fetchFixtures(token, accountId) +
                    api.fetchRefereeFixtures(token, accountId)
                ).distinctBy { it.id to it.role }
            database.inTransaction {
                database.fixtureDao().deleteForAccount(accountId)
                database.fixtureDao().upsertAll(fixtures.map { it.toEntity(accountId) })
                markUpdated(FIXTURES_KEY, accountId)
            }
        }
    }

    override suspend fun refreshFamilyProfiles(
            profiles: List<FamilyProfile>,
            force: Boolean,
        ): Map<String, RefreshResult> {
            val rootToken = store.getBearerToken() ?: return profiles.associate {
                it.id to RefreshResult.Failed(RefreshFailure.Unauthorized, hasCachedData = false)
            }
            val rootAccountId = profiles.primaryProfileId()
                ?: return profiles.associate {
                    it.id to RefreshResult.Failed(
                        RefreshFailure.InvalidResponse,
                        hasCachedData = false,
                    )
                }
            return profiles.associate { profile ->
                val existingCount = database.fixtureDao().countForAccount(profile.id)
                val metadata = database.cacheMetadataDao().get(FIXTURES_KEY, profile.id)
                val refreshSports = force || metadata.isStale(FIXTURES_TTL)
                val hydrateProfile = !profile.isPrimary
                val result = if (!refreshSports && !hydrateProfile) {
                    RefreshResult.NotModified
                } else {
                    val refreshResult = runRefresh(existingCount > 0) {
                        val token = if (profile.isPrimary) {
                            rootToken
                        } else {
                            api.createProfileSession(rootToken, profile.id)
                        }
                        if (refreshSports) {
                            val teams = api.fetchProfileTeams(token)
                            val fixtures = (
                                api.fetchFixtures(token, profile.id) +
                                    api.fetchRefereeFixtures(token, profile.id)
                                ).distinctBy { it.id to it.role }
                            val clubs = api.fetchProfileClubs(token)
                            database.inTransaction {
                                database.fixtureDao().deleteForAccount(profile.id)
                                database.fixtureDao()
                                    .upsertAll(fixtures.map { it.toEntity(profile.id) })
                                database.teamDao().deleteRelationships(profile.id)
                                database.teamDao().upsertAll(teams.map { it.team.toEntity() })
                                teams.forEach { team ->
                                    database.teamDao().upsertRelationship(
                                        TeamRelationshipEntity(
                                            accountId = profile.id,
                                            teamId = team.team.id,
                                            relationship = team.relationship.name,
                                        )
                                    )
                                }
                                database.clubDao().deleteRelationships(profile.id)
                                database.clubDao().upsertAll(clubs.map { it.toEntity() })
                                database.clubDao().upsertRelationships(
                                    clubs.map { ClubRelationshipEntity(profile.id, it.id) }
                                )
                                markUpdated(FIXTURES_KEY, profile.id)
                            }
                        }

                        if (hydrateProfile) {
                            hydratePersonalInformation(
                                rootAccountId = rootAccountId,
                                profileId = profile.id,
                                bearerToken = token,
                            )
                        }
                    }

                    if (!refreshSports && refreshResult == RefreshResult.Updated) {
                        RefreshResult.NotModified
                    } else {
                        refreshResult
                    }
                }
                profile.id to result
            }
    }

    private suspend fun hydratePersonalInformation(
            rootAccountId: String,
            profileId: String,
            bearerToken: String,
    ) {
            val personal = api.fetchPersonalInformation(bearerToken, profileId)
            val personalEntity = personal.toLinkedEntity(rootAccountId)
            val contactEndpointContacts = api.fetchEmergencyContacts(bearerToken)
            val existing = database.profileDao().getRelatedUser(rootAccountId, profileId)
                ?: return
            val emergencyContacts = mergeEmergencyContacts(
                personalInformationContacts = personal.emergencyContacts,
                contactEndpointContacts = contactEndpointContacts,
                existingContacts = existing.emergencyContactsJson,
            )
            database.profileDao().upsertRelatedUsers(
                listOf(
                    existing.copy(
                        displayName = personalEntity.displayName.takeUnless {
                            it == personalEntity.id
                        }
                            ?: existing.displayName,
                        email = personalEntity.email ?: existing.email,
                        avatarUrl = personalEntity.avatarUrl ?: existing.avatarUrl,
                        gender = personalEntity.gender ?: existing.gender,
                        dateOfBirth = personalEntity.dateOfBirth ?: existing.dateOfBirth,
                        phoneNumber = personalEntity.phoneNumber ?: existing.phoneNumber,
                        address = personalEntity.address ?: existing.address,
                        emergencyContactsJson = emergencyContacts,
                    )
                )
            )
    }

    override suspend fun setFollowing(teamId: String, following: Boolean) {
        val accountId = store.getActiveAccountId() ?: return
        if (following) {
            database.teamDao().upsertRelationship(
                TeamRelationshipEntity(accountId, teamId, TeamRelationship.Following.name)
            )
        } else {
            database.teamDao().deleteRelationship(accountId, teamId)
        }
    }

    override suspend fun loadTeamHub(
        profileId: String,
        teamId: String,
        force: Boolean,
    ): CachedData<TeamHub> {
        val cached = database.teamHubCacheDao().get(profileId, teamId)
        if (!force && cached != null) {
            return CachedData(
                value = Json.decodeFromString(cached.payloadJson),
                lastUpdatedEpochMillis = cached.updatedAtEpochMillis,
                isStale = timeProvider.nowEpochMillis() - cached.updatedAtEpochMillis >=
                    TEAM_HUB_TTL,
            )
        }
        val hub = api.fetchTeamHub(profileToken(profileId), teamId).let { loaded ->
            loaded.copy(
                matches = loaded.matches.map { it.copy(profileId = profileId) },
                results = loaded.results.map { it.copy(profileId = profileId) },
            )
        }
        val updatedAt = timeProvider.nowEpochMillis()
        database.teamHubCacheDao().upsert(
            TeamHubCacheEntity(
                profileId = profileId,
                teamId = teamId,
                payloadJson = Json.encodeToString(hub),
                updatedAtEpochMillis = updatedAt,
            )
        )
        return CachedData(hub, updatedAt, isStale = false)
    }

    override suspend fun loadMatchDetails(profileId: String, matchId: String): Fixture =
        api.fetchMatchDetails(profileToken(profileId), matchId).copy(profileId = profileId)

    private suspend fun profileToken(profileId: String): String {
        val rootToken = store.getBearerToken()
            ?: throw DriblResponseException("No authenticated session")
        val rootProfileId = store.getActiveAccountId()
            ?: throw DriblResponseException("No active profile")
        return if (profileId == rootProfileId) {
            rootToken
        } else {
            api.createProfileSession(rootToken, profileId)
        }
    }

    private suspend fun refreshPublic(
        key: String,
        ttl: Long,
        force: Boolean,
        existingCount: Int,
        scope: String = PUBLIC_SCOPE,
        block: suspend (String) -> Unit,
    ): RefreshResult {
        val token = store.getBearerToken()
            ?: return RefreshResult.Failed(RefreshFailure.Unauthorized, existingCount > 0)
        val metadata = database.cacheMetadataDao().get(key, scope)
        if (!force && existingCount > 0 && !metadata.isStale(ttl)) {
            return RefreshResult.NotModified
        }
        return runRefresh(existingCount > 0) { block(token) }
    }

    private suspend fun runRefresh(
        hasCachedData: Boolean,
        block: suspend () -> Unit,
    ): RefreshResult =
        try {
            block()
            RefreshResult.Updated
        } catch (error: DriblHttpException) {
            RefreshResult.Failed(
                reason = when (error.statusCode) {
                    401 -> RefreshFailure.Unauthorized
                    403 -> RefreshFailure.Forbidden
                    in 500..599 -> RefreshFailure.Server
                    else -> RefreshFailure.InvalidResponse
                },
                hasCachedData = hasCachedData,
            )
        } catch (_: DriblResponseException) {
            RefreshResult.Failed(RefreshFailure.InvalidResponse, hasCachedData)
        } catch (_: SerializationException) {
            RefreshResult.Failed(RefreshFailure.InvalidResponse, hasCachedData)
        } catch (_: IOException) {
            RefreshResult.Failed(RefreshFailure.Offline, hasCachedData)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            RefreshResult.Failed(RefreshFailure.Unknown, hasCachedData)
        }

    private suspend fun markUpdated(key: String, accountScope: String) {
        database.cacheMetadataDao().upsert(
            CacheMetadataEntity(key, accountScope, timeProvider.nowEpochMillis())
        )
    }

    private fun CacheMetadataEntity?.isStale(ttlMillis: Long): Boolean =
        this == null || timeProvider.nowEpochMillis() - updatedAtEpochMillis >= ttlMillis

    private fun Flow<String?>.flatMapAccountFixtures(): Flow<CachedData<List<Fixture>>> =
        flatMapLatest { accountId ->
            if (accountId == null) {
                kotlinx.coroutines.flow.flowOf(CachedData(emptyList(), null, true))
            } else {
                combine(
                    database.fixtureDao().observeForAccount(accountId),
                    database.cacheMetadataDao().observe(FIXTURES_KEY, accountId),
                ) { fixtures, metadata ->
                    CachedData(
                        fixtures.map { it.toDomain() }
                            .upcomingAt(timeProvider.nowEpochMillis()),
                        metadata?.updatedAtEpochMillis,
                        metadata.isStale(FIXTURES_TTL),
                    )
                }
            }

        }

    private fun Flow<String?>.flatMapRelationships(): Flow<Map<String, TeamRelationship>> =
        flatMapLatest { accountId ->
            if (accountId == null) {
                kotlinx.coroutines.flow.flowOf(emptyMap())
            } else {
                database.teamDao().observeRelationships(accountId).map { relationships ->
                    relationships.associate { it.teamId to relationshipFrom(it.relationship) }
                }
            }
        }

    private fun relationshipFrom(value: String): TeamRelationship =
        TeamRelationship.entries.firstOrNull { it.name == value } ?: TeamRelationship.None

    private companion object {
        const val PUBLIC_SCOPE = "public"
        const val CLUBS_KEY = "clubs"
        const val TEAMS_KEY = "teams.current-season.v1"
        const val FIXTURES_KEY = "fixtures"
        const val CLUBS_TTL = 24L * 60L * 60L * 1_000L
        const val TEAMS_TTL = 24L * 60L * 60L * 1_000L
        const val FIXTURES_TTL = 15L * 60L * 1_000L
        const val TEAM_HUB_TTL = 24L * 60L * 60L * 1_000L
    }
}

internal fun List<Fixture>.upcomingAt(nowEpochMillis: Long): List<Fixture> =
    filter { fixture ->
        when {
            fixture.kickoffEpochMillis >= nowEpochMillis -> true
            fixture.status == FixtureStatus.Live -> true
            fixture.status in finishedFixtureStatuses -> false
            else ->
                fixture.kickoffEpochMillis + MaximumFixtureVisibilityMillis >
                    nowEpochMillis
        }
    }

private val finishedFixtureStatuses = setOf(
    FixtureStatus.Completed,
    FixtureStatus.Cancelled,
    FixtureStatus.Washout,
)
private const val MaximumFixtureVisibilityMillis = 3L * 60L * 60L * 1_000L
