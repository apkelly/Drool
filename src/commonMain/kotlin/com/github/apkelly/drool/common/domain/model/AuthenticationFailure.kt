package com.github.apkelly.drool.common.domain.model

enum class AuthenticationFailure {
    InvalidCredentials,
    Offline,
    Unknown,
}

class AuthenticationException(
    val reason: AuthenticationFailure,
    cause: Throwable,
) : Exception(cause)
