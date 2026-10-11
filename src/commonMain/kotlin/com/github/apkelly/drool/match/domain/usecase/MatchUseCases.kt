package com.github.apkelly.drool.match.domain.usecase

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.match.domain.repository.MatchRepository

class LoadMatchDetailsUseCase(
    private val repository: MatchRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        matchId: String,
    ) = repository.loadMatchDetails(profileId, matchId)
}

class LoadMatchWeatherUseCase(
    private val repository: MatchRepository,
) {
    suspend operator fun invoke(
        fixture: Fixture,
    ) = repository.loadMatchWeather(fixture)
}
