package com.github.apkelly.drool.profile.data

import com.github.apkelly.drool.common.data.repository.SessionDataSource
import com.github.apkelly.drool.profile.domain.repository.ProfileRepository

class ProfileRepositoryImpl(
    private val sessionDataSource: SessionDataSource,
) : ProfileRepository {
    override suspend fun getLinkCandidates() = sessionDataSource.getLinkCandidates()

    override suspend fun requestLinkedUser(
        candidateId: String,
        isGuardian: Boolean,
    ) = sessionDataSource.requestLinkedUser(candidateId, isGuardian)

    override suspend fun verifyLinkedUser(
        candidateId: String,
        token: String,
    ) = sessionDataSource.verifyLinkedUser(candidateId, token)

    override suspend fun unlinkLinkedUser(
        linkId: String,
    ) = sessionDataSource.unlinkLinkedUser(linkId)
}
