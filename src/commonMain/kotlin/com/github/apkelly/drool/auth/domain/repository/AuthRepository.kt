package com.github.apkelly.drool.auth.domain.repository

import kotlinx.coroutines.flow.Flow
import com.github.apkelly.drool.domain.model.AuthCredentials
import com.github.apkelly.drool.domain.model.AuthenticatedSession
import com.github.apkelly.drool.domain.model.Profile

interface AuthRepository {
    fun observeProfile(): Flow<Profile?>
    suspend fun restoreSession(): AuthenticatedSession?
    suspend fun signIn(credentials: AuthCredentials): AuthenticatedSession
    suspend fun signOut()
}
