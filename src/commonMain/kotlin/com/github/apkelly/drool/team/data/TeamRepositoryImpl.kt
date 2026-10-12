package com.github.apkelly.drool.team.data

import com.github.apkelly.dribl.api.SportsRemoteDataSource
import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.local.entity.TeamHubCacheEntity
import com.github.apkelly.drool.common.data.mapper.toDomain
import com.github.apkelly.drool.common.data.repository.operationResult
import com.github.apkelly.drool.common.data.repository.profileToken
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.data.time.TimeProvider
import com.github.apkelly.drool.common.domain.model.CachedData
import com.github.apkelly.drool.common.domain.model.OperationResult
import com.github.apkelly.drool.common.domain.model.TeamHub
import com.github.apkelly.drool.team.domain.repository.TeamRepository
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TeamRepositoryImpl(
    private val api: SportsRemoteDataSource,
    private val database: DroolDatabase,
    private val store: BearerTokenStore,
    private val timeProvider: TimeProvider,
) : TeamRepository {
    override suspend fun loadTeamHub(
        profileId: String,
        teamId: String,
        force: Boolean,
    ): OperationResult<CachedData<TeamHub>> = operationResult {
        val cached = database.teamHubCacheDao().get(profileId, teamId)
        if (!force && cached != null) {
            return@operationResult CachedData(
                value = Json.decodeFromString(cached.payloadJson),
                lastUpdatedEpochMillis = cached.updatedAtEpochMillis,
                isStale = timeProvider.nowEpochMillis() - cached.updatedAtEpochMillis >=
                    TEAM_HUB_TTL,
            )
        }
        val hub = api.fetchTeamHub(profileToken(api, store, profileId), teamId)
            .toDomain()
            .let { loaded ->
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
            ),
        )
        CachedData(hub, updatedAt, isStale = false)
    }

    private companion object {
        const val TEAM_HUB_TTL = 24L * 60L * 60L * 1_000L
    }
}
