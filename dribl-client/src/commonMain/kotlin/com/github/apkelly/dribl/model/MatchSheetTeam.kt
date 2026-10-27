package com.github.apkelly.dribl.model

data class MatchSheetTeam(
    val id: String,
    val name: String,
    val status: TeamSheetStatus,
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
)
