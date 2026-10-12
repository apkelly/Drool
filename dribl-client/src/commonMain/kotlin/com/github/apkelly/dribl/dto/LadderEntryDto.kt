package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class LadderEntryDto(
    @SerialName("position") val position: Int? = null,
    @SerialName("team_id") val teamId: JsonPrimitive? = null,
    @SerialName("team_hash_id") val teamHashId: JsonPrimitive? = null,
    @SerialName("team_name") val teamName: String? = null,
    @SerialName("league_name") val leagueName: String? = null,
    @SerialName("club_logo") val clubLogo: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("played") val played: Int? = null,
    @SerialName("won") val won: Int? = null,
    @SerialName("drawn") val drawn: Int? = null,
    @SerialName("lost") val lost: Int? = null,
    @SerialName("goals_for") val goalsFor: Int? = null,
    @SerialName("goals_against") val goalsAgainst: Int? = null,
    @SerialName("goal_difference") val goalDifference: Int? = null,
    @SerialName("points") val points: Int? = null,
    @SerialName("upcoming_matches") val upcomingMatches: List<FixtureDto> = emptyList(),
    @SerialName("recent_matches") val recentMatches: List<FixtureDto> = emptyList(),
)
