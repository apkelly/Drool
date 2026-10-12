package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ResourceList<T>(
    @SerialName("data") val items: List<T> = emptyList(),
)
