package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class AccountDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("account_id") val accountId: JsonPrimitive? = null,
    @SerialName("account_name") val accountName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("value") val value: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("attributes") val attributes: AccountAttributesDto? = null,
)
