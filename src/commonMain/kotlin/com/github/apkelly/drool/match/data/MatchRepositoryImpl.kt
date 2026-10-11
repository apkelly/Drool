package com.github.apkelly.drool.match.data

import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.match.data.weather.MatchWeatherDataSource
import com.github.apkelly.drool.data.repository.SportsDataSource
import com.github.apkelly.drool.match.domain.repository.MatchRepository

class MatchRepositoryImpl(
    private val sportsDataSource: SportsDataSource,
    private val weatherDataSource: MatchWeatherDataSource,
) : MatchRepository {
    override suspend fun loadMatchDetails(profileId: String, matchId: String) =
        sportsDataSource.loadMatchDetails(profileId, matchId)

    override suspend fun loadMatchWeather(fixture: Fixture) =
        weatherDataSource.loadMatchWeather(fixture)
}
