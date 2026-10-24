package com.github.apkelly.drool.match.ui.model

import com.github.apkelly.drool.common.domain.model.MatchSheet
import com.github.apkelly.drool.match.domain.model.RefereeMatchSession

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
        val checkingInRefereeId: String? = null,
        val isSubmittingRefereeReport: Boolean = false,
        val refereeReportSubmitted: Boolean = false,
        val refereeSession: RefereeMatchSession = RefereeMatchSession(
            profileId = profileId,
            matchId = matchId,
        ),
        val updateFailed: Boolean = false,
    ) : MatchSheetUiState
    data class Failed(
        val profileId: String,
        val matchId: String,
    ) : MatchSheetUiState
}
