package com.github.apkelly.drool.team.ui.model

import com.github.apkelly.drool.domain.model.RefreshFailure
import com.github.apkelly.drool.domain.model.TeamHub

sealed interface TeamHubUiState {
    data object Idle : TeamHubUiState
    data class Loading(val profileId: String, val teamId: String) : TeamHubUiState
    data class Content(
        val profileId: String,
        val teamId: String,
        val hub: TeamHub,
        val lastUpdatedEpochMillis: Long?,
        val isStale: Boolean,
        val isRefreshing: Boolean = false,
        val refreshFailure: RefreshFailure? = null,
    ) : TeamHubUiState
    data class Failed(val profileId: String, val teamId: String) : TeamHubUiState
}
