package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class TeamReferenceDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("team_id") val teamId: JsonPrimitive? = null,
    @SerialName("teamId") val teamIdCamel: JsonPrimitive? = null,
    @SerialName("name") val name: String? = null,
)
