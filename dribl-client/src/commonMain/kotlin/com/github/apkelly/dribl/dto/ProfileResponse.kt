package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileResponse(
    @SerialName("data") val data: ProfilePayload? = null,
    @SerialName("profile") val profile: PersonDto? = null,
)
