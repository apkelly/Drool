package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LadderDetailResponse(
    @SerialName("ladder_entries") val ladderEntries: List<LadderEntryResourceDto> = emptyList(),
)
