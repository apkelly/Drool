package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatchRoleResourceDto(
    @SerialName("attributes") val attributes: MatchRoleAttributesDto,
)
