package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatchRoleAttributesDto(
    @SerialName("name") val name: String? = null,
    @SerialName("slug") val slug: String? = null,
)
