package com.github.apkelly.drool.domain.repository

import kotlinx.coroutines.flow.Flow
import com.github.apkelly.drool.domain.model.AuthCredentials
import com.github.apkelly.drool.domain.model.AuthenticatedSession
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.domain.model.RelatedUser

interface SessionRepository {
    fun observeProfile(): Flow<Profile?>
    suspend fun restoreSession(): AuthenticatedSession?
    suspend fun signIn(credentials: AuthCredentials): AuthenticatedSession
    suspend fun refreshProfileEndpoint(endpoint: ProfileApiEndpoint, email: String)
    suspend fun getLinkCandidates(): List<RelatedUser>
    suspend fun verifyLinkedUser(candidateId: String, token: String)
    suspend fun signOut()
}
