package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class MatchSheetOfficialAttributesDto(
    @SerialName("user_id") val userId: JsonPrimitive? = null,
    @SerialName("user_first_name") val userFirstName: String? = null,
    @SerialName("user_last_name") val userLastName: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("referee_name") val refereeName: String? = null,
    @SerialName("role_name") val roleName: String? = null,
    @SerialName("referee_role") val refereeRole: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("presence") val presence: String? = null,
)
