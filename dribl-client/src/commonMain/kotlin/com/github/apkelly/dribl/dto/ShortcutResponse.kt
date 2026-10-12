package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShortcutResponse(
    @SerialName("teams") val teams: List<TeamResourceDto> = emptyList(),
)
