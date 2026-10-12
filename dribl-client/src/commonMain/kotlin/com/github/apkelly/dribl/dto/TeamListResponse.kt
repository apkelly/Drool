package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TeamListResponse(
    @SerialName("data") val data: List<TeamResourceDto>,
    @SerialName("meta") val meta: PaginationMetaDto? = null,
)
