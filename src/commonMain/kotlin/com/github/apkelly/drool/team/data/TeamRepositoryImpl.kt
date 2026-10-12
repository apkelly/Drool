package com.github.apkelly.drool.team.data

import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.local.entity.TeamHubCacheEntity
import com.github.apkelly.drool.common.data.remote.SportsRemoteDataSource
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
        competitionName: String?,
        leagueId: String?,
        force: Boolean,
    ): OperationResult<CachedData<TeamHub>> = operationResult {
        val cached = database.teamHubCacheDao().get(profileId, teamId)
        if (!force) {
            cached?.let {
                val cachedHub = Json.decodeFromString<TeamHub>(it.payloadJson)
                if (cachedHub.ladderName.matchesCompetition(competitionName)) {
                    return@operationResult CachedData(
                        value = cachedHub,
                        lastUpdatedEpochMillis = it.updatedAtEpochMillis,
                        isStale = timeProvider.nowEpochMillis() - it.updatedAtEpochMillis >=
                            TEAM_HUB_TTL,
                    )
                }
            }
        }
        val hub = api.fetchTeamHub(
            profileToken(api, store, profileId),
            teamId,
            competitionName,
            leagueId,
        ).let { loaded ->
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

private fun String?.matchesCompetition(
    competitionName: String?,
): Boolean = competitionName.isNullOrBlank() ||
    (!isNullOrBlank() && trim().equals(competitionName.trim(), ignoreCase = true))
