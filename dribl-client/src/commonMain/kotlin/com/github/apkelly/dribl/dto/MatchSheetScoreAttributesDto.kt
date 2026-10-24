package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatchSheetScoreAttributesDto(
    @SerialName("home_half_time") val homeHalfTime: Int? = null,
    @SerialName("away_half_time") val awayHalfTime: Int? = null,
    @SerialName("home_full_time") val homeFullTime: Int? = null,
    @SerialName("away_full_time") val awayFullTime: Int? = null,
)
