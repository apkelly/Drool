package com.github.apkelly.drool.domain.model

enum class AuthenticationFailure {
    InvalidCredentials,
    Offline,
    Unknown,
}

class AuthenticationException(
    val reason: AuthenticationFailure,
    cause: Throwable,
) : Exception(cause)
