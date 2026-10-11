package com.github.apkelly.drool.profile.domain.repository

import com.github.apkelly.drool.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.domain.model.RelatedUser

interface ProfileRepository {
    suspend fun refreshProfileEndpoint(endpoint: ProfileApiEndpoint, email: String)
    suspend fun getLinkCandidates(): List<RelatedUser>
    suspend fun verifyLinkedUser(candidateId: String, token: String)
}
