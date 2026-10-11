package com.github.apkelly.dribl.model

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
