package com.github.apkelly.drool.match.domain.repository

import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.MatchWeather

interface MatchRepository {
    suspend fun loadMatchDetails(profileId: String, matchId: String): Fixture
    suspend fun loadMatchWeather(fixture: Fixture): MatchWeather?
}
