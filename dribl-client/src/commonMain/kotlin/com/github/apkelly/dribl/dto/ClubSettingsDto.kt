package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ClubSettingsDto(
    @SerialName("timezone") val timeZone: String? = null,
)
