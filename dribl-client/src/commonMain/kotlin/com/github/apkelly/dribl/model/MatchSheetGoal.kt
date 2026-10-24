package com.github.apkelly.dribl.model

data class MatchSheetGoal(
    val id: String,
    val teamId: String? = null,
    val matchSheetMemberId: String? = null,
    val memberName: String? = null,
    val jerseyNumber: String? = null,
    val half: MatchHalf = MatchHalf.FirstHalf,
    val minute: Int? = null,
    val ownGoal: Boolean = false,
    val penaltyKick: Boolean = false,
)
