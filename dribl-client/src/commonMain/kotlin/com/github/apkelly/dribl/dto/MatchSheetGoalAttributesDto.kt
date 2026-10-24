package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class MatchSheetGoalAttributesDto(
    @SerialName("team_id") val teamId: JsonPrimitive? = null,
    @SerialName("matchsheet_member_id") val matchSheetMemberId: JsonPrimitive? = null,
    @SerialName("matchsheet_member_name") val matchSheetMemberName: String? = null,
    @SerialName("jersey_number") val jerseyNumber: JsonPrimitive? = null,
    @SerialName("game_section") val gameSection: String? = null,
    @SerialName("minute") val minute: Int? = null,
    @SerialName("own_goal") val ownGoal: Boolean = false,
    @SerialName("penalty_kick") val penaltyKick: Boolean = false,
)
