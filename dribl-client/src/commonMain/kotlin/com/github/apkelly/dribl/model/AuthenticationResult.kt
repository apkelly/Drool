package com.github.apkelly.dribl.model

data class AuthenticationResult(
    val bearerToken: String,
    val identity: AuthenticationIdentity? = null,
    val relatedUsers: List<RelatedUser> = emptyList(),
    val accounts: List<Account> = emptyList(),
)
