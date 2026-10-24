package com.github.apkelly.dribl.model

data class MatchSheetGoalDraft(
    val matchSheetMemberId: String,
    val half: MatchHalf,
    val minute: Int? = null,
    val ownGoal: Boolean = false,
    val penaltyKick: Boolean = false,
)
