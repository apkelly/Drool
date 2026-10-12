package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NestedResourceResponse<T>(
    @SerialName("data") val data: ResourceList<T>? = null,
)
