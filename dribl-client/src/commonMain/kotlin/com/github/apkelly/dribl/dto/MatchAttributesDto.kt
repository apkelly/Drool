package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class MatchAttributesDto(
    @SerialName("match_id") val matchId: JsonPrimitive? = null,
    @SerialName("matchsheet_id") val matchSheetId: JsonPrimitive? = null,
    @SerialName("date") val date: JsonPrimitive,
    @SerialName("home_team_id") val homeTeamId: JsonPrimitive,
    @SerialName("home_team_name") val homeTeamName: String,
    @SerialName("away_team_id") val awayTeamId: JsonPrimitive,
    @SerialName("away_team_name") val awayTeamName: String,
    @SerialName("competition_name") val competitionName: String? = null,
    @SerialName("league_name") val leagueName: String? = null,
    @SerialName("league_id") val leagueId: JsonPrimitive? = null,
    @SerialName("season_id") val seasonId: JsonPrimitive? = null,
    @SerialName("tenant_id") val tenantId: JsonPrimitive? = null,
    @SerialName("field_name") val fieldName: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("latitude") val latitude: JsonPrimitive? = null,
    @SerialName("longitude") val longitude: JsonPrimitive? = null,
    @SerialName("round_label") val roundLabel: String? = null,
    @SerialName("round_number") val roundNumber: JsonPrimitive? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("home_team_score") val homeTeamScore: Int? = null,
    @SerialName("away_team_score") val awayTeamScore: Int? = null,
    @SerialName("home_team_logo") val homeTeamLogo: String? = null,
    @SerialName("away_team_logo") val awayTeamLogo: String? = null,
    @SerialName("roles") val roles: List<MatchRoleResourceDto> = emptyList(),
)
