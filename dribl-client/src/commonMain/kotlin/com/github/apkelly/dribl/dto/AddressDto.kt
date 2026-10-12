package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AddressDto(
    @SerialName("line_1") val line1: String? = null,
    @SerialName("line_2") val line2: String? = null,
    @SerialName("suburb") val suburb: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("postcode") val postcode: String? = null,
)
