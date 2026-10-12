package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class ClubDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("club_id") val clubId: JsonPrimitive? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("club_name") val clubName: String? = null,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("primary_color") val primaryColor: String? = null,
    @SerialName("secondary_color") val secondaryColor: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("accent") val accent: String? = null,
    @SerialName("settings") val settings: ClubSettingsDto? = null,
)
