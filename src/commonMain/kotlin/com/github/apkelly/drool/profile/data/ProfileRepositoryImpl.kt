package com.github.apkelly.drool.profile.data

import com.github.apkelly.drool.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.data.repository.SessionDataSource
import com.github.apkelly.drool.profile.domain.repository.ProfileRepository

class ProfileRepositoryImpl(
    private val sessionDataSource: SessionDataSource,
) : ProfileRepository {
    override suspend fun refreshProfileEndpoint(
        endpoint: ProfileApiEndpoint,
        email: String,
    ) = sessionDataSource.refreshProfileEndpoint(endpoint, email)

    override suspend fun getLinkCandidates() = sessionDataSource.getLinkCandidates()

    override suspend fun verifyLinkedUser(candidateId: String, token: String) =
        sessionDataSource.verifyLinkedUser(candidateId, token)
}
