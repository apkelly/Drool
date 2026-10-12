package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class LadderEntryResourceDto(
    @SerialName("attributes") val attributes: LadderEntryDto? = null,
    @SerialName("position") val position: Int? = null,
    @SerialName("team_hash_id") val teamId: JsonPrimitive? = null,
    @SerialName("team_name") val teamName: String? = null,
)
