package com.github.apkelly.drool.match.ui.model

import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.MatchWeather

sealed interface MatchDetailsUiState {
    data object Idle : MatchDetailsUiState
    data class Loading(
        val profileId: String,
        val matchId: String,
    ) : MatchDetailsUiState
    data class Content(
        val profileId: String,
        val matchId: String,
        val fixture: Fixture,
        val weather: MatchWeather? = null,
        val isWeatherLoading: Boolean = false,
        val weatherUnavailable: Boolean = false,
    ) : MatchDetailsUiState
    data class Failed(
        val profileId: String,
        val matchId: String,
    ) : MatchDetailsUiState
}
