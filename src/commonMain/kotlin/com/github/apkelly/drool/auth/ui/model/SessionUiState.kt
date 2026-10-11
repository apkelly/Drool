package com.github.apkelly.drool.auth.ui.model

import com.github.apkelly.drool.domain.model.Profile

sealed interface SessionUiState {
    data object Bootstrapping : SessionUiState
    data object AuthenticationRequired : SessionUiState
    data object ReconnectRequired : SessionUiState
    data object Authenticating : SessionUiState
    data class Authenticated(
        val profile: Profile,
    ) : SessionUiState
    data class AuthenticationFailed(
        val reason: AuthFailure,
    ) : SessionUiState
}

enum class AuthFailure {
    MissingFields,
    InvalidCredentials,
    Offline,
    Unknown,
}
