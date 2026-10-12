package com.github.apkelly.dribl.model

data class Profile(
    val accountId: String,
    val displayName: String,
    val email: String?,
    val avatarUrl: String? = null,
    val relatedUsers: List<RelatedUser> = emptyList(),
    val accounts: List<Account> = emptyList(),
)
