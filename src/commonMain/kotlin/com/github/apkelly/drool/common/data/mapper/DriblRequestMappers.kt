package com.github.apkelly.drool.common.data.mapper

import com.github.apkelly.dribl.dto.AuthApiRequest
import com.github.apkelly.drool.common.domain.model.AuthCredentials

internal fun AuthCredentials.toDriblRequest() = AuthApiRequest(
    baseUrl = baseUrl,
    path = path,
    username = username,
    password = password,
)
