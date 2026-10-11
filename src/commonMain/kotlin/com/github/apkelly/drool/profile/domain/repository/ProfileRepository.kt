package com.github.apkelly.drool.profile.domain.repository

import com.github.apkelly.drool.common.domain.model.LinkMemberRequestResult
import com.github.apkelly.drool.common.domain.model.RelatedUser

interface ProfileRepository {
    suspend fun getLinkCandidates(): List<RelatedUser>
    suspend fun requestLinkedUser(
        candidateId: String,
        isGuardian: Boolean,
    ): LinkMemberRequestResult
    suspend fun verifyLinkedUser(
        candidateId: String,
        token: String,
    )
    suspend fun unlinkLinkedUser(
        linkId: String,
    )
}
