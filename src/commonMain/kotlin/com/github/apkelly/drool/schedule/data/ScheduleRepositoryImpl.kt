package com.github.apkelly.drool.schedule.data

import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.local.entity.ClubRelationshipEntity
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
                    CachedData(
                        value = fixtures.map { it.toDomain() }
                            .recentAndUpcomingAt(timeProvider.nowEpochMillis()),
                        lastUpdatedEpochMillis = metadata?.updatedAtEpochMillis,
                        isStale = metadata.isStale(
                            timeProvider.nowEpochMillis(),
                            FIXTURES_TTL,
                        ),
                    )
                }
            }
        }

    override fun observeLinkedProfileFixtures(
        profileIds: Set<String>,
    ): Flow<List<Fixture>> = database.fixtureDao()
        .observeForAccounts(profileIds.toList())
        .map { fixtures ->
            fixtures.map { it.toDomain() }
                .recentAndUpcomingAt(timeProvider.nowEpochMillis())
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
            val fixtures = (
                api.fetchFixtures(token, accountId) +
                    loadRefereeFixtures(token, accountId)
                ).distinctBy { it.id to it.role }
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
            val refreshSports = force ||
                metadata.isStale(timeProvider.nowEpochMillis(), FIXTURES_TTL)
            val hydrateProfile = !profile.isPrimary
            val result = if (!refreshSports && !hydrateProfile) {
                RefreshResult.NotModified
            } else {
                val result = refreshResult(existingCount > 0) {
                    val token = if (profile.isPrimary) {
                        rootToken
                    } else {
                        api.createProfileSession(rootToken, profile.id)
                    }
                    if (refreshSports) {
                        val teams = api.fetchProfileTeams(token)
                        val fixtures = (
                            api.fetchFixtures(token, profile.subjectUserId) +
                                loadRefereeFixtures(token, profile.subjectUserId)
                            ).distinctBy { it.id to it.role }
                        val clubs = api.fetchProfileClubs(token)
                        database.inTransaction {
                            database.fixtureDao().deleteForAccount(profile.id)
                            database.fixtureDao().upsertAll(
                                fixtures.map { it.toEntity(profile.id) },
                            )
                            database.teamDao().deleteRelationships(profile.id)
                            database.teamDao().upsertAll(teams.map { it.team.toEntity() })
                            teams.forEach { team ->
                                database.teamDao().upsertRelationship(
                                    TeamRelationshipEntity(
                                        accountId = profile.id,
                                        teamId = team.team.id,
                                        relationship = team.relationship.name,
                                    ),
                                )
                            }
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
                    }

                    if (hydrateProfile) {
                        hydratePersonalInformation(
                            rootAccountId = rootAccountId,
                            profileId = profile.id,
                            bearerToken = token,
                        )
                    }
                }

                if (!refreshSports && result == RefreshResult.Updated) {
                    RefreshResult.NotModified
                } else {
                    result
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

    private suspend fun loadRefereeFixtures(
        bearerToken: String,
        userId: String,
    ): List<Fixture> {
        if (api.fetchActiveRefereeClubs(bearerToken).isEmpty()) return emptyList()
        return api.fetchRefereeFixtures(bearerToken, userId)
    }

    private suspend fun hydratePersonalInformation(
        rootAccountId: String,
        profileId: String,
        bearerToken: String,
    ) {
        val personal = api.fetchPersonalInformation(bearerToken, profileId)
        val personalEntity = personal.toLinkedEntity(rootAccountId)
        val contactEndpointContacts =
            api.fetchEmergencyContacts(bearerToken)
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

    private companion object {
        const val FIXTURES_KEY = "fixtures"
        const val FIXTURES_TTL = 15L * 60L * 1_000L
    }
}
