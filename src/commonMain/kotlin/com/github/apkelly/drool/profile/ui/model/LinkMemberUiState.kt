package com.github.apkelly.drool.profile.ui.model

import com.github.apkelly.drool.common.domain.model.LinkMemberFailure
import com.github.apkelly.drool.common.domain.model.RelatedUser

sealed interface LinkMemberUiState {
    data object Idle : LinkMemberUiState
    data object Loading : LinkMemberUiState
    data class Ready(
        val candidates: List<RelatedUser>,
        val requestingCandidateId: String? = null,
    ) : LinkMemberUiState

    data class AwaitingVerification(
        val candidates: List<RelatedUser>,
        val candidate: RelatedUser,
        val linkId: String,
        val verifying: Boolean = false,
    ) : LinkMemberUiState

    data class Failed(
        val reason: LinkMemberFailure,
    ) : LinkMemberUiState
    data object Linked : LinkMemberUiState
}
