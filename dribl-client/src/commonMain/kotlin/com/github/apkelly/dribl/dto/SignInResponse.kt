package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SignInResponse(
    @SerialName("status") val status: Int,
    @SerialName("token") val token: String,
    @SerialName("user") val user: PersonDto,
    @SerialName("refresh_token") val refreshToken: String? = null,
)
