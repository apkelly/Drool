package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class ScheduleAllocationAttributesDto(
    @SerialName("event_id") val eventId: JsonPrimitive? = null,
    @SerialName("match_id") val matchId: JsonPrimitive? = null,
    @SerialName("date") val date: JsonPrimitive? = null,
    @SerialName("start_date") val startDate: JsonPrimitive? = null,
    @SerialName("home_team_id") val homeTeamId: JsonPrimitive? = null,
    @SerialName("home_club_name") val homeClubName: String? = null,
    @SerialName("home_team_name") val homeTeamName: String? = null,
    @SerialName("away_team_id") val awayTeamId: JsonPrimitive? = null,
    @SerialName("away_club_name") val awayClubName: String? = null,
    @SerialName("away_team_name") val awayTeamName: String? = null,
    @SerialName("home_club_image") val homeClubImage: String? = null,
    @SerialName("away_club_image") val awayClubImage: String? = null,
    @SerialName("competition_name") val competitionName: String? = null,
    @SerialName("league_name") val leagueName: String? = null,
    @SerialName("ground") val ground: String? = null,
    @SerialName("field") val field: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("referee_role") val refereeRole: String? = null,
    @SerialName("referee_status") val refereeStatus: String? = null,
)
