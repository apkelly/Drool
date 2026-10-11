package com.github.apkelly.drool.profile.ui.model

import com.github.apkelly.drool.domain.model.LinkMemberFailure
import com.github.apkelly.drool.domain.model.RelatedUser

sealed interface LinkMemberUiState {
    data object Idle : LinkMemberUiState
    data object Loading : LinkMemberUiState
    data class Ready(
        val candidates: List<RelatedUser>,
        val verifyingCandidateId: String? = null,
    ) : LinkMemberUiState
    data class Failed(val reason: LinkMemberFailure) : LinkMemberUiState
    data object Linked : LinkMemberUiState
}
