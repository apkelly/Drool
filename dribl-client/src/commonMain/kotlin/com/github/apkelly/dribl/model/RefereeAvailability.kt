package com.github.apkelly.dribl.model

data class RefereeAvailability(
    val id: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val clubId: String?,
    val clubName: String?,
    val period: AvailabilityPeriod,
    val templateEndEpochMillis: Long?,
)
