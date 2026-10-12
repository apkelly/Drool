package com.github.apkelly.drool.match.data

import com.github.apkelly.drool.common.data.remote.SportsRemoteDataSource
import com.github.apkelly.drool.common.data.repository.operationResult
import com.github.apkelly.drool.common.data.repository.profileToken
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
import com.github.apkelly.drool.match.data.weather.MatchWeatherDataSource
import com.github.apkelly.drool.match.domain.repository.MatchRepository

class MatchRepositoryImpl(
    private val api: SportsRemoteDataSource,
    private val store: BearerTokenStore,
    private val weatherDataSource: MatchWeatherDataSource,
) : MatchRepository {
    override suspend fun loadMatchDetails(
        profileId: String,
        matchId: String,
    ) = operationResult {
        api.fetchMatchDetails(
            profileToken(api, store, profileId),
            matchId,
        ).copy(profileId = profileId)
    }

    override suspend fun loadMatchWeather(
        fixture: Fixture,
        force: Boolean,
    ) = operationResult {
        weatherDataSource.loadMatchWeather(fixture, force)
    }

    override suspend fun loadMatchSheet(
        profileId: String,
        fixture: Fixture,
    ) = operationResult {
        api.fetchMatchSheet(
            profileToken(api, store, profileId),
            fixture,
        )
    }

    override suspend fun updateMemberAvailability(
        profileId: String,
        memberId: String,
        available: Boolean,
    ) = operationResult {
        api.updateMatchSheetMemberAvailability(
            profileToken(api, store, profileId),
            memberId,
            available,
        )
    }

    override suspend fun updateTeamSheetStatus(
        profileId: String,
        matchSheetId: String,
        teamId: String,
        action: TeamSheetAction,
    ) = operationResult {
        api.updateTeamSheetStatus(
            bearerToken = profileToken(api, store, profileId),
            matchSheetId = matchSheetId,
            teamId = teamId,
            action = action,
        )
    }
}
