package com.github.apkelly.dribl.model

data class MatchSheetMember(
    val id: String,
    val userId: String?,
    val teamId: String?,
    val firstName: String,
    val lastName: String,
    val imageUrl: String?,
    val roleName: String,
    val jerseyNumber: String?,
    val available: Boolean,
)
