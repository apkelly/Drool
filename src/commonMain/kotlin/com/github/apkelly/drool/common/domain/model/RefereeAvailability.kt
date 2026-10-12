package com.github.apkelly.drool.common.domain.model

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
