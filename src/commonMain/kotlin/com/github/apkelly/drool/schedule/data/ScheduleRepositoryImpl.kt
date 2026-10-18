package com.github.apkelly.drool.schedule.data

import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.local.entity.CacheMetadataEntity
import com.github.apkelly.drool.common.data.local.entity.ClubRelationshipEntity
import com.github.apkelly.drool.common.data.local.entity.FixtureEntity
import com.github.apkelly.drool.common.data.local.entity.TeamRelationshipEntity
import com.github.apkelly.drool.common.data.local.inTransaction
import com.github.apkelly.drool.common.data.mapper.mergeEmergencyContacts
import com.github.apkelly.drool.common.data.mapper.toDomain
import com.github.apkelly.drool.common.data.mapper.toEntity
import com.github.apkelly.drool.common.data.mapper.toLinkedEntity
import com.github.apkelly.drool.common.data.remote.SportsRemoteDataSource
import com.github.apkelly.drool.common.data.repository.isStale
import com.github.apkelly.drool.common.data.repository.markUpdated
import com.github.apkelly.drool.common.data.repository.recentAndUpcomingAt
import com.github.apkelly.drool.common.data.repository.refreshResult
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.data.time.TimeProvider
import com.github.apkelly.drool.common.domain.model.CachedData
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.common.domain.model.RefereeAllocationDecision
import com.github.apkelly.drool.common.domain.model.RefreshFailure
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.domain.model.primaryProfileId
import com.github.apkelly.drool.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleRepositoryImpl(
    private val api: SportsRemoteDataSource,
    private val database: DroolDatabase,
    private val store: BearerTokenStore,
    private val timeProvider: TimeProvider,
) : ScheduleRepository {
    override fun observeFixtures(): Flow<CachedData<List<Fixture>>> = database.profileDao().observeActive()
        .map { it?.accountId }
        .flatMapLatest { accountId ->
            if (accountId == null) {
                flowOf(CachedData(emptyList(), null, true))
            } else {
                combine(
                    database.fixtureDao().observeForAccount(accountId),
                    database.cacheMetadataDao().observe(FIXTURES_KEY, accountId),
                ) { fixtures, metadata ->
                    cachedFixtures(fixtures, metadata)
                }
            }
        }

    override fun observeLinkedProfileFixtures(
        profileIds: Set<String>,
    ): Flow<CachedData<List<Fixture>>> = combine(
        database.fixtureDao().observeForAccounts(profileIds.toList()),
        database.cacheMetadataDao().observeOldestForAccounts(FIXTURES_KEY, profileIds.toList()),
    ) { fixtures, metadata ->
        cachedFixtures(fixtures, metadata)
    }

    override suspend fun refreshFixtures(
        force: Boolean,
    ): RefreshResult {
        val token = store.getBearerToken()
            ?: return RefreshResult.Failed(RefreshFailure.Unauthorized, hasCachedData = false)
        val accountId = store.getActiveAccountId()
            ?: return RefreshResult.Failed(RefreshFailure.Unauthorized, hasCachedData = false)
        val existingCount = database.fixtureDao().countForAccount(accountId)
        val metadata = database.cacheMetadataDao().get(FIXTURES_KEY, accountId)
        if (
            !force &&
            !metadata.isStale(timeProvider.nowEpochMillis(), FIXTURES_TTL)
        ) {
            return RefreshResult.NotModified
        }

        return refreshResult(existingCount > 0) {
            val fixtures = coroutineScope {
                val playerFixtures = async { api.fetchFixtures(token, accountId) }
                val refereeFixtures = async { loadRefereeFixtures(token, accountId) }
                (playerFixtures.await() + refereeFixtures.await())
                    .distinctBy { it.id to it.role }
            }
            database.inTransaction {
                database.fixtureDao().deleteForAccount(accountId)
                database.fixtureDao().upsertAll(fixtures.map { it.toEntity(accountId) })
                database.markUpdated(
                    FIXTURES_KEY,
                    accountId,
                    timeProvider.nowEpochMillis(),
                )
            }
        }
    }

    override suspend fun refreshLinkedProfiles(
        profiles: List<LinkedProfile>,
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
            val refreshProfile = force ||
                metadata.isStale(timeProvider.nowEpochMillis(), FIXTURES_TTL)
            val result = if (!refreshProfile) {
                RefreshResult.NotModified
            } else {
                refreshResult(existingCount > 0) {
                    val token = if (profile.isPrimary) {
                        rootToken
                    } else {
                        api.createProfileSession(rootToken, profile.id)
                    }
                    val (teams, fixtures, clubs) = coroutineScope {
                        val teamsDeferred = async { api.fetchProfileTeams(token) }
                        val playerFixturesDeferred = async {
                            api.fetchFixtures(token, profile.subjectUserId)
                        }
                        val refereeFixturesDeferred = async {
                            loadRefereeFixtures(token, profile.subjectUserId)
                        }
                        val clubsDeferred = async { api.fetchProfileClubs(token) }
                        Triple(
                            teamsDeferred.await(),
                            (playerFixturesDeferred.await() + refereeFixturesDeferred.await())
                                .distinctBy { it.id to it.role },
                            clubsDeferred.await(),
                        )
                    }
                    database.inTransaction {
                        database.fixtureDao().deleteForAccount(profile.id)
                        database.fixtureDao().upsertAll(
                            fixtures.map { it.toEntity(profile.id) },
                        )
                        database.teamDao().deleteServerRelationships(profile.id)
                        database.teamDao().upsertAll(teams.map { it.team.toEntity() })
                        database.teamDao().upsertRelationships(
                            teams.map { team ->
                                TeamRelationshipEntity(
                                    accountId = profile.id,
                                    teamId = team.team.id,
                                    relationship = team.relationship.name,
                                )
                            },
                        )
                        database.clubDao().deleteRelationships(profile.id)
                        database.clubDao().upsertAll(clubs.map { it.toEntity() })
                        database.clubDao().upsertRelationships(
                            clubs.map { ClubRelationshipEntity(profile.id, it.id) },
                        )
                        database.markUpdated(
                            FIXTURES_KEY,
                            profile.id,
                            timeProvider.nowEpochMillis(),
                        )
                    }

                    if (!profile.isPrimary) {
                        hydratePersonalInformation(
                            rootAccountId = rootAccountId,
                            profileId = profile.id,
                            bearerToken = token,
                        )
                    }
                }
            }
            profile.id to result
        }
    }

    override suspend fun updateRefereeAllocation(
        profile: LinkedProfile,
        allocationId: String,
        decision: RefereeAllocationDecision,
    ): RefreshResult {
        val rootToken = store.getBearerToken()
            ?: return RefreshResult.Failed(RefreshFailure.Unauthorized, hasCachedData = true)
        return refreshResult(hasCachedData = true) {
            val token = if (profile.isPrimary) {
                rootToken
            } else {
                api.createProfileSession(rootToken, profile.id)
            }
            api.updateRefereeAllocation(
                bearerToken = token,
                allocationId = allocationId,
                status = when (decision) {
                    RefereeAllocationDecision.Accept -> "accepted"
                    RefereeAllocationDecision.Decline -> "request_contact"
                },
            )
        }
    }

    private fun cachedFixtures(
        fixtures: List<FixtureEntity>,
        metadata: CacheMetadataEntity?,
    ): CachedData<List<Fixture>> {
        val now = timeProvider.nowEpochMillis()
        return CachedData(
            value = fixtures.map { it.toDomain() }
                .recentAndUpcomingAt(now),
            lastUpdatedEpochMillis = metadata?.updatedAtEpochMillis,
            isStale = metadata.isStale(now, FIXTURES_TTL),
        )
    }

    private suspend fun loadRefereeFixtures(
        bearerToken: String,
        userId: String,
    ): List<Fixture> = api.fetchRefereeFixtures(bearerToken, userId)

    private suspend fun hydratePersonalInformation(
        rootAccountId: String,
        profileId: String,
        bearerToken: String,
    ) {
        val (personal, contactEndpointContacts) = coroutineScope {
            val personalDeferred = async { api.fetchPersonalInformation(bearerToken, profileId) }
            val contactsDeferred = async { api.fetchEmergencyContacts(bearerToken) }
            personalDeferred.await() to contactsDeferred.await()
        }
        val personalEntity = personal.toLinkedEntity(rootAccountId)
        database.inTransaction {
            val existing = database.profileDao().getRelatedUser(rootAccountId, profileId)
                ?: return@inTransaction
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
                        } ?: existing.displayName,
                        email = personalEntity.email ?: existing.email,
                        avatarUrl = personalEntity.avatarUrl ?: existing.avatarUrl,
                        gender = personalEntity.gender ?: existing.gender,
                        dateOfBirth = personalEntity.dateOfBirth ?: existing.dateOfBirth,
                        phoneNumber = personalEntity.phoneNumber ?: existing.phoneNumber,
                        address = personalEntity.address ?: existing.address,
                        emergencyContactsJson = emergencyContacts,
                    ),
                ),
            )
        }
    }

    private companion object {
        const val FIXTURES_KEY = "fixtures"
        const val FIXTURES_TTL = 15L * 60L * 1_000L
    }
}
