package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatchSheetMemberAvailabilityRequest(
    @SerialName("available") val available: Boolean,
    @SerialName("starting") val starting: Boolean? = null,
    @SerialName("playing") val playing: Boolean? = null,
    @SerialName("is_captain") val isCaptain: Boolean? = null,
    @SerialName("is_goalkeeper") val isGoalkeeper: Boolean? = null,
)
