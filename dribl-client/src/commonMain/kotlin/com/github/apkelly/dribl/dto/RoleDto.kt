package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RoleDto(
    @SerialName("slug") val slug: String? = null,
    @SerialName("name") val name: String? = null,
)
