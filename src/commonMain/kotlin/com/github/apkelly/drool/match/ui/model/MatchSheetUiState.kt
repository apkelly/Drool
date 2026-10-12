package com.github.apkelly.drool.match.ui.model

import com.github.apkelly.drool.common.domain.model.MatchSheet

sealed interface MatchSheetUiState {
    data object Idle : MatchSheetUiState
    data class Loading(
        val profileId: String,
        val matchId: String,
    ) : MatchSheetUiState
    data class Content(
        val profileId: String,
        val matchId: String,
        val matchSheet: MatchSheet,
        val updatingMemberId: String? = null,
        val updatingTeamId: String? = null,
        val updateFailed: Boolean = false,
    ) : MatchSheetUiState
    data class Failed(
        val profileId: String,
        val matchId: String,
    ) : MatchSheetUiState
}
