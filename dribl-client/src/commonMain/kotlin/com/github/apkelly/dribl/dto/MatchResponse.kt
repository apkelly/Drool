package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatchResponse(
    @SerialName("data") val data: MatchResourceDto,
)
