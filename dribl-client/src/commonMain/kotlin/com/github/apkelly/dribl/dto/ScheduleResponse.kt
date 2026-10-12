package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ScheduleResponse(
    @SerialName("allocations") val allocations: List<ScheduleAllocationResourceDto> = emptyList(),
    @SerialName("availabilities")
    val availabilities: List<RefereeAvailabilityResourceDto> = emptyList(),
)
