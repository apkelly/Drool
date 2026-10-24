package com.github.apkelly.dribl.model

data class MatchSheet(
    val id: String,
    val status: String?,
    val homeTeam: MatchSheetTeam,
    val awayTeam: MatchSheetTeam,
    val members: List<MatchSheetMember>,
    val referees: List<MatchSheetOfficial>,
    val officials: List<MatchSheetOfficial>,
    val score: MatchSheetScore? = null,
    val goals: List<MatchSheetGoal> = emptyList(),
)
