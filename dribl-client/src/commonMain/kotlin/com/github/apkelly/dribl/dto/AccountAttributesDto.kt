package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AccountAttributesDto(
    @SerialName("name") val name: String? = null,
    @SerialName("value") val value: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("organisation_name") val organisationName: String? = null,
    @SerialName("activated") val activated: Boolean? = null,
)
