package com.github.apkelly.drool.discover.data

import com.github.apkelly.dribl.api.SportsRemoteDataSource
import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.local.entity.TeamRelationshipEntity
import com.github.apkelly.drool.common.data.local.inTransaction
import com.github.apkelly.drool.common.data.mapper.toDomain
import com.github.apkelly.drool.common.data.mapper.toEntity
import com.github.apkelly.drool.common.data.repository.isStale
import com.github.apkelly.drool.common.data.repository.markUpdated
import com.github.apkelly.drool.common.data.repository.refreshResult
import com.github.apkelly.drool.common.data.repository.relationshipFrom
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.data.time.TimeProvider
import com.github.apkelly.drool.common.domain.model.CachedData
import com.github.apkelly.drool.common.domain.model.RefreshFailure
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import com.github.apkelly.drool.discover.domain.repository.DiscoverRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverRepositoryImpl(
    private val api: SportsRemoteDataSource,
    private val database: DroolDatabase,
    private val store: BearerTokenStore,
    private val timeProvider: TimeProvider,
) : DiscoverRepository {
    override fun observeClubs() = combine(
        database.clubDao().observeAll(),
        database.cacheMetadataDao().observe(CLUBS_KEY, PUBLIC_SCOPE),
    ) { clubs, metadata ->
        CachedData(
            value = clubs.map { it.toDomain() },
            lastUpdatedEpochMillis = metadata?.updatedAtEpochMillis,
            isStale = metadata.isStale(timeProvider.nowEpochMillis(), CLUBS_TTL),
        )
    }

    override fun observeTeams(
        clubId: String?,
    ) = combine(
        if (clubId == null) {
            database.teamDao().observeAll()
        } else {
            database.teamDao().observeByClub(clubId)
        },
        database.cacheMetadataDao().observe(TEAMS_KEY, clubId ?: PUBLIC_SCOPE),
    ) { teams, metadata ->
        CachedData(
            value = teams.map { it.toDomain() },
            lastUpdatedEpochMillis = metadata?.updatedAtEpochMillis,
            isStale = metadata.isStale(timeProvider.nowEpochMillis(), TEAMS_TTL),
        )
    }

    override fun observeTeamRelationships() = database.profileDao().observeActive()
        .map { it?.accountId }
        .flatMapLatest { accountId ->
            if (accountId == null) {
                flowOf(emptyMap())
            } else {
                database.teamDao().observeRelationships(accountId).map { relationships ->
                    relationships.associate {
                        it.teamId to relationshipFrom(it.relationship)
                    }
                }
            }
        }

    override suspend fun refreshClubs(
        force: Boolean,
    ): RefreshResult = refreshPublic(
        key = CLUBS_KEY,
        ttl = CLUBS_TTL,
        force = force,
        existingCount = database.clubDao().count(),
    ) { token ->
        val clubs = api.fetchClubs(token).map { it.toDomain() }
        database.inTransaction {
            database.clubDao().deleteAll()
            database.clubDao().upsertAll(clubs.map { it.toEntity() })
            database.markUpdated(CLUBS_KEY, PUBLIC_SCOPE, timeProvider.nowEpochMillis())
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
            val teams = api.fetchTeams(token, clubId).map { it.toDomain() }
            database.inTransaction {
                if (clubId != null) {
                    database.teamDao().deleteUnrelatedForClub(clubId)
                }
                database.teamDao().upsertAll(teams.map { it.toEntity() })
                database.markUpdated(TEAMS_KEY, scope, timeProvider.nowEpochMillis())
            }
        }
    }

    override suspend fun setFollowing(
        teamId: String,
        following: Boolean,
    ) {
        val accountId = store.getActiveAccountId() ?: return
        if (following) {
            database.teamDao().upsertRelationship(
                TeamRelationshipEntity(accountId, teamId, TeamRelationship.Following.name),
            )
        } else {
            database.teamDao().deleteRelationship(accountId, teamId)
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
        if (
            !force &&
            existingCount > 0 &&
            !metadata.isStale(timeProvider.nowEpochMillis(), ttl)
        ) {
            return RefreshResult.NotModified
        }
        return refreshResult(existingCount > 0) { block(token) }
    }

    private companion object {
        const val PUBLIC_SCOPE = "public"
        const val CLUBS_KEY = "clubs"
        const val TEAMS_KEY = "teams.current-season.v1"
        const val CLUBS_TTL = 24L * 60L * 60L * 1_000L
        const val TEAMS_TTL = 24L * 60L * 60L * 1_000L
    }
}
