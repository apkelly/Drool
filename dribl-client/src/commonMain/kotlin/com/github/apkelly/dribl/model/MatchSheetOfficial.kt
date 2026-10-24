package com.github.apkelly.dribl.model

data class MatchSheetOfficial(
    val id: String,
    val displayName: String,
    val roleName: String?,
    val userId: String? = null,
    val checkedIn: Boolean = false,
)
