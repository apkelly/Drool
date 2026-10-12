package com.github.apkelly.drool.common.data.remote

import com.github.apkelly.dribl.dto.AuthApiRequest
import com.github.apkelly.drool.common.data.mapper.toDomain
import com.github.apkelly.drool.common.domain.model.AuthenticationResult

internal class DriblAuthRemoteDataSource(
    private val delegate: com.github.apkelly.dribl.api.AuthRemoteDataSource,
) : AuthRemoteDataSource {
    override suspend fun signIn(
        request: AuthApiRequest,
    ): AuthenticationResult = delegate.signIn(request).toDomain()
}
