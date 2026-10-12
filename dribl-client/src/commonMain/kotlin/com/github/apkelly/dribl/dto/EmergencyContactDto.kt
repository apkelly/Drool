package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EmergencyContactDto(
    @SerialName("name") val name: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("contact_name") val contactName: String? = null,
    @SerialName("phone_number_1") val phoneNumber1: String? = null,
    @SerialName("phone_number_2") val phoneNumber2: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("mobile") val mobile: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("contact_email") val contactEmail: String? = null,
)
