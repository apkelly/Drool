package com.github.apkelly.drool.auth.data

import com.github.apkelly.drool.auth.domain.repository.AuthRepository
import com.github.apkelly.drool.common.data.repository.SessionDataSource
import com.github.apkelly.drool.common.domain.model.AuthCredentials

class AuthRepositoryImpl(
    private val sessionDataSource: SessionDataSource,
) : AuthRepository {
    override fun observeProfile() = sessionDataSource.observeProfile()

    override suspend fun restoreSession() = sessionDataSource.restoreSession()

    override suspend fun signIn(
        credentials: AuthCredentials,
    ) = sessionDataSource.signIn(credentials)

    override suspend fun signOut() = sessionDataSource.signOut()
}
