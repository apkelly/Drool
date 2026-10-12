package com.github.apkelly.dribl.api

import com.github.apkelly.dribl.dto.AuthApiRequest
import com.github.apkelly.dribl.model.AuthenticationResult

interface AuthRemoteDataSource {
    suspend fun signIn(
        request: AuthApiRequest,
    ): AuthenticationResult
}
