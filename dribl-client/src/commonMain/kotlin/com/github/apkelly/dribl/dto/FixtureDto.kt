package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class FixtureDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("match_id") val matchId: JsonPrimitive? = null,
    @SerialName("kickoff_at") val kickoffAt: JsonPrimitive? = null,
    @SerialName("date") val date: JsonPrimitive? = null,
    @SerialName("home_team") val homeTeam: TeamReferenceDto? = null,
    @SerialName("home_team_id") val homeTeamId: JsonPrimitive? = null,
    @SerialName("home_team_hash_id") val homeTeamHashId: JsonPrimitive? = null,
    @SerialName("home_team_name") val homeTeamName: String? = null,
    @SerialName("away_team") val awayTeam: TeamReferenceDto? = null,
    @SerialName("away_team_id") val awayTeamId: JsonPrimitive? = null,
    @SerialName("away_team_hash_id") val awayTeamHashId: JsonPrimitive? = null,
    @SerialName("away_team_name") val awayTeamName: String? = null,
    @SerialName("competition_name") val competitionName: String? = null,
    @SerialName("league_name") val leagueName: String? = null,
    @SerialName("venue_name") val venueName: String? = null,
    @SerialName("ground_name") val groundName: String? = null,
    @SerialName("user_team_id") val userTeamId: JsonPrimitive? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("home_score") val homeScore: Int? = null,
    @SerialName("away_score") val awayScore: Int? = null,
    @SerialName("home_team_logo") val homeTeamLogo: String? = null,
    @SerialName("home_club_image") val homeClubImage: String? = null,
    @SerialName("away_team_logo") val awayTeamLogo: String? = null,
    @SerialName("away_club_image") val awayClubImage: String? = null,
)
