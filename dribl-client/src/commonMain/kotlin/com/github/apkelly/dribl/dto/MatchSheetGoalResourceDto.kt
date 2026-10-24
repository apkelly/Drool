package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class MatchSheetGoalResourceDto(
    @SerialName("id") val id: JsonPrimitive,
    @SerialName("attributes") val attributes: MatchSheetGoalAttributesDto = MatchSheetGoalAttributesDto(),
)
