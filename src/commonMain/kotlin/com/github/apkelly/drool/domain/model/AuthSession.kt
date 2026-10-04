package com.github.apkelly.drool.domain.model

data class AuthenticationResult(
    val bearerToken: String,
    val identity: AuthenticationIdentity? = null,
    val relatedUsers: List<RelatedUser> = emptyList(),
    val accounts: List<Account> = emptyList(),
) {
    val firstName: String?
        get() = identity?.firstName
}

data class AuthenticationIdentity(
    val id: String?,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val avatarUrl: String?,
)
