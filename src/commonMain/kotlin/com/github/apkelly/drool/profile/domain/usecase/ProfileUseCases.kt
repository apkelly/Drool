package com.github.apkelly.drool.profile.domain.usecase

import com.github.apkelly.drool.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.profile.domain.repository.ProfileRepository

class RefreshProfileEndpointUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(
        endpoint: ProfileApiEndpoint,
        email: String,
    ) = repository.refreshProfileEndpoint(endpoint, email)
}

class GetLinkCandidatesUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke() = repository.getLinkCandidates()
}

class VerifyLinkedUserUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(
        candidateId: String,
        token: String,
    ) = repository.verifyLinkedUser(candidateId, token)
}
