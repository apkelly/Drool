package com.github.apkelly.drool.match.data

import com.github.apkelly.dribl.api.SportsRemoteDataSource
import com.github.apkelly.drool.common.data.mapper.toDomain
import com.github.apkelly.drool.common.data.repository.operationResult
import com.github.apkelly.drool.common.data.repository.profileToken
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.domain.model.Fixture
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
        ).toDomain().copy(profileId = profileId)
    }

    override suspend fun loadMatchWeather(
        fixture: Fixture,
        force: Boolean,
    ) = operationResult {
        weatherDataSource.loadMatchWeather(fixture, force)
    }
}
