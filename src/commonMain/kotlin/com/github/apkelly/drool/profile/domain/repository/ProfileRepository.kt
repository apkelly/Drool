package com.github.apkelly.drool.profile.domain.repository

import com.github.apkelly.drool.common.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.common.domain.model.RelatedUser

interface ProfileRepository {
    suspend fun refreshProfileEndpoint(
        endpoint: ProfileApiEndpoint,
        email: String,
    )
    suspend fun getLinkCandidates(): List<RelatedUser>
    suspend fun verifyLinkedUser(
        candidateId: String,
        token: String,
    )
}
