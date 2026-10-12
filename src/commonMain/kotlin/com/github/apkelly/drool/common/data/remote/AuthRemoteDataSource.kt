package com.github.apkelly.drool.common.data.remote

import com.github.apkelly.dribl.dto.AuthApiRequest
import com.github.apkelly.drool.common.domain.model.AuthenticationResult

interface AuthRemoteDataSource {
    suspend fun signIn(
        request: AuthApiRequest,
    ): AuthenticationResult
}
