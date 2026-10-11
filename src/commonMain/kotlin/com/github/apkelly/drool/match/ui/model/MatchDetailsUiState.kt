package com.github.apkelly.drool.match.ui.model

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.MatchWeather

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
        val isRefreshing: Boolean = false,
        val refreshFailed: Boolean = false,
    ) : MatchDetailsUiState
    data class Failed(
        val profileId: String,
        val matchId: String,
    ) : MatchDetailsUiState
}
