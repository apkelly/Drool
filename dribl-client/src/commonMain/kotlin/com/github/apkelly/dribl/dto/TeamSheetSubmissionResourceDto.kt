package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class TeamSheetSubmissionResourceDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("attributes") val attributes: TeamSheetSubmissionAttributesDto,
)
