package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class MatchSheetScoreResourceDto(
    @SerialName("id") val id: JsonPrimitive,
    @SerialName("attributes") val attributes: MatchSheetScoreAttributesDto = MatchSheetScoreAttributesDto(),
)
