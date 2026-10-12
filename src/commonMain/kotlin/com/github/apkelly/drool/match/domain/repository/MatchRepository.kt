package com.github.apkelly.drool.match.domain.repository

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.MatchWeather
import com.github.apkelly.drool.common.domain.model.OperationResult

interface MatchRepository {
    suspend fun loadMatchDetails(
        profileId: String,
        matchId: String,
    ): OperationResult<Fixture>
    suspend fun loadMatchWeather(
        fixture: Fixture,
        force: Boolean = false,
    ): OperationResult<MatchWeather?>
}
