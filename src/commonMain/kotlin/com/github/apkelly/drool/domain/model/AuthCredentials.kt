package com.github.apkelly.drool.domain.model

data class AuthCredentials(
    val baseUrl: String,
    val path: String,
    val username: String,
    val password: String,
)
