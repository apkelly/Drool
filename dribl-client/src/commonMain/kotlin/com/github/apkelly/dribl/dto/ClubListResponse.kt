package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ClubListResponse(
    @SerialName("data") val data: List<ClubResourceDto>,
    @SerialName("meta") val meta: PaginationMetaDto? = null,
)
