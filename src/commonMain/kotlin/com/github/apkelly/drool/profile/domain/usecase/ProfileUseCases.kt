package com.github.apkelly.drool.profile.domain.usecase

import com.github.apkelly.drool.profile.domain.repository.ProfileRepository

class GetLinkCandidatesUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke() = repository.getLinkCandidates()
}

class RequestLinkedUserUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(
        candidateId: String,
        isGuardian: Boolean,
    ) = repository.requestLinkedUser(candidateId, isGuardian)
}

class VerifyLinkedUserUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(
        candidateId: String,
        token: String,
    ) = repository.verifyLinkedUser(candidateId, token)
}

class UnlinkLinkedUserUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(
        linkId: String,
    ) = repository.unlinkLinkedUser(linkId)
}
