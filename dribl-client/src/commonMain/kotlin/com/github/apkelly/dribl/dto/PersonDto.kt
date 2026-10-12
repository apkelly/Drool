package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class PersonDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("user_id") val userId: JsonPrimitive? = null,
    @SerialName("account_id") val accountId: JsonPrimitive? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("primary_email") val primaryEmail: String? = null,
    @SerialName("sending_email_address") val sendingEmailAddress: String? = null,
    @SerialName("email_address") val emailAddress: String? = null,
    @SerialName("contact_email") val contactEmail: String? = null,
    @SerialName("value") val value: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("profile_image") val profileImage: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("system_image") val systemImage: String? = null,
    @SerialName("dob") val dateOfBirth: String? = null,
    @SerialName("date_of_birth") val alternateDateOfBirth: String? = null,
    @SerialName("activated") val activated: Boolean? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("self_id") val selfId: JsonPrimitive? = null,
    @SerialName("is_guardian") val isGuardian: Int? = null,
    @SerialName("gender") val gender: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("mobile") val mobile: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("mobile_number") val mobileNumber: String? = null,
    @SerialName("address") val address: AddressDto? = null,
    @SerialName("address_line_1") val addressLine1: String? = null,
    @SerialName("address_line_2") val addressLine2: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("postcode") val postcode: String? = null,
    @SerialName("emergency_contacts")
    val emergencyContacts: List<EmergencyContactDto> = emptyList(),
)
