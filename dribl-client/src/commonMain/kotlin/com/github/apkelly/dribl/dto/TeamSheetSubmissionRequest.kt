package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TeamSheetSubmissionRequest(
    @SerialName("matchsheet_id") val matchSheetId: String,
    @SerialName("team_id") val teamId: String,
    @SerialName("type") val type: String,
)
