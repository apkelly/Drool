package com.github.apkelly.drool.ui.model

import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.RefreshFailure
import com.github.apkelly.drool.domain.model.RelatedUser
import com.github.apkelly.drool.domain.model.LinkMemberFailure

sealed interface SessionUiState {
    data object Bootstrapping : SessionUiState
    data object AuthenticationRequired : SessionUiState
    data object ReconnectRequired : SessionUiState
    data object Authenticating : SessionUiState
    data class Authenticated(val profile: Profile) : SessionUiState
    data class AuthenticationFailed(val reason: AuthFailure) : SessionUiState
}

enum class AuthFailure {
    MissingFields,
    InvalidCredentials,
    Offline,
    Unknown,
}

data class CollectionUiState<T>(
    val items: List<T> = emptyList(),
    val lastUpdatedEpochMillis: Long? = null,
    val isStale: Boolean = true,
    val isRefreshing: Boolean = false,
    val refreshFailure: RefreshFailure? = null,
)

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
