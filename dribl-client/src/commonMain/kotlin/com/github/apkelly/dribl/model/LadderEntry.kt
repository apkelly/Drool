package com.github.apkelly.dribl.model

data class LadderEntry(
    val position: Int?,
    val teamId: String?,
    val teamName: String,
    val logoUrl: String?,
    val played: Int?,
    val won: Int?,
    val drawn: Int?,
    val lost: Int?,
    val goalsFor: Int?,
    val goalsAgainst: Int?,
    val goalDifference: Int?,
    val points: Int?,
)
