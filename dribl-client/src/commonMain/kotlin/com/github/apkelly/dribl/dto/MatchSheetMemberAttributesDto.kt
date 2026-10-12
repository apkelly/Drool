package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class MatchSheetMemberAttributesDto(
    @SerialName("user_id") val userId: JsonPrimitive? = null,
    @SerialName("team_id") val teamId: JsonPrimitive? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("first_name_alias") val firstNameAlias: String? = null,
    @SerialName("last_name_alias") val lastNameAlias: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("role_name") val roleName: String? = null,
    @SerialName("jersey_number") val jerseyNumber: JsonPrimitive? = null,
    @SerialName("available") val available: Boolean = false,
)
