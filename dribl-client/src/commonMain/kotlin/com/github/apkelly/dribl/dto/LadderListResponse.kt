package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LadderListResponse(
    @SerialName("data") val data: List<LadderSummaryDto> = emptyList(),
    @SerialName("ladders") val ladders: List<LadderSummaryDto> = emptyList(),
)
