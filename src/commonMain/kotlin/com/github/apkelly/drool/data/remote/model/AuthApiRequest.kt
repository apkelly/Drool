package com.github.apkelly.drool.data.remote.model

import com.github.apkelly.drool.domain.model.AuthCredentials

data class AuthApiRequest(
    val baseUrl: String,
    val path: String,
    val username: String,
    val password: String,
    val usernameField: String = "username",
    val passwordField: String = "password",
) {
    val url: String = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}

fun AuthCredentials.toApiRequest(): AuthApiRequest =
    AuthApiRequest(
        baseUrl = baseUrl,
        path = path,
        username = username,
        password = password,
    )
