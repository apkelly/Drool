package com.github.apkelly.dribl.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class RefereeClub(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val timeZone: String,
)

data class RefereeAvailability(
    val id: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val clubId: String?,
    val clubName: String?,
    val period: AvailabilityPeriod,
    val templateEndEpochMillis: Long?,
)

enum class AvailabilityPeriod {
    SingleDay,
    Weekly,
    Fortnightly,
    Monthly,
}

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
