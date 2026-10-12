package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class TeamDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("team_id") val teamId: JsonPrimitive? = null,
    @SerialName("club_id") val clubId: JsonPrimitive? = null,
    @SerialName("club_name") val clubName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("parsed_name") val parsedName: String? = null,
    @SerialName("team_name") val teamName: String? = null,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("age_group") val ageGroup: String? = null,
    @SerialName("competition_name") val competitionName: String? = null,
    @SerialName("team_competition_name") val teamCompetitionName: String? = null,
    @SerialName("active") val active: Boolean? = null,
    @SerialName("primary_color") val primaryColor: String? = null,
    @SerialName("secondary_color") val secondaryColor: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("accent") val accent: String? = null,
    @SerialName("roles") val roles: List<RoleResourceDto> = emptyList(),
)
