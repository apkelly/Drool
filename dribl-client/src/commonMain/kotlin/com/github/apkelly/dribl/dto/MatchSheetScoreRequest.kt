package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatchSheetScoreRequest(
    @SerialName("matchsheet_id") val matchSheetId: String,
    @SerialName("home_half_time") val homeHalfTime: Int? = null,
    @SerialName("away_half_time") val awayHalfTime: Int? = null,
    @SerialName("home_full_time") val homeFullTime: Int,
    @SerialName("away_full_time") val awayFullTime: Int,
    @SerialName("home_extra_half_time") val homeExtraHalfTime: Int? = null,
    @SerialName("away_extra_half_time") val awayExtraHalfTime: Int? = null,
    @SerialName("home_extra_full_time") val homeExtraFullTime: Int? = null,
    @SerialName("away_extra_full_time") val awayExtraFullTime: Int? = null,
    @SerialName("home_penalties") val homePenalties: Int? = null,
    @SerialName("away_penalties") val awayPenalties: Int? = null,
    @SerialName("teamforfeit") val teamForfeit: String? = null,
)
