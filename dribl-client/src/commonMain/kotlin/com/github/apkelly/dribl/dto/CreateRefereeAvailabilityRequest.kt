package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateRefereeAvailabilityRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("all_clubs") val allClubs: Boolean,
    @SerialName("club_id") val clubId: String?,
    @SerialName("period") val period: String,
    @SerialName("day") val day: Int?,
    @SerialName("use_timezone") val timeZone: String,
    @SerialName("note") val note: String? = null,
)
