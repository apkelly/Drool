package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RefereeAvailabilityResponse(
    @SerialName("data") val data: RefereeAvailabilityResourceDto,
)
