package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatchSheetGoalRequest(
    @SerialName("matchsheet_id") val matchSheetId: String,
    @SerialName("matchsheet_member_id") val matchSheetMemberId: String,
    @SerialName("game_section") val gameSection: String,
    @SerialName("minute") val minute: Int? = null,
    @SerialName("own_goal") val ownGoal: Boolean = false,
    @SerialName("penalty_kick") val penaltyKick: Boolean = false,
)
