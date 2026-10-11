package com.github.apkelly.drool.common.data.repository

import com.github.apkelly.drool.common.domain.model.AuthCredentials
import com.github.apkelly.drool.common.domain.model.AuthenticatedSession
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.common.domain.model.RelatedUser
import kotlinx.coroutines.flow.Flow

interface SessionDataSource {
    fun observeProfile(): Flow<Profile?>
    suspend fun restoreSession(): AuthenticatedSession?
    suspend fun signIn(
        credentials: AuthCredentials,
    ): AuthenticatedSession
    suspend fun refreshProfileEndpoint(
        endpoint: ProfileApiEndpoint,
        email: String,
    )
    suspend fun getLinkCandidates(): List<RelatedUser>
    suspend fun verifyLinkedUser(
        candidateId: String,
        token: String,
    )
    suspend fun signOut()
}
