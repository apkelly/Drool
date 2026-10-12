package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LinkUserRequest(
    @SerialName("related_user") val relatedUser: LinkUserSelection,
    @SerialName("approve") val approve: Boolean,
)
