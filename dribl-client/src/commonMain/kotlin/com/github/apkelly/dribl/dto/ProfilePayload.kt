package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ProfilePayload(
    @SerialName("account") val account: PersonDto? = null,
    @SerialName("profile") val profile: PersonDto? = null,
    @SerialName("user") val user: PersonDto? = null,
)
