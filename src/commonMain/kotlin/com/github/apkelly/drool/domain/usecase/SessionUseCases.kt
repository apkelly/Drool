package com.github.apkelly.drool.domain.usecase

import com.github.apkelly.drool.domain.model.AuthCredentials
import com.github.apkelly.drool.domain.repository.SessionRepository
import com.github.apkelly.drool.domain.model.ProfileApiEndpoint

class ObserveProfileUseCase(private val repository: SessionRepository) {
    operator fun invoke() = repository.observeProfile()
}

class RestoreSessionUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke() = repository.restoreSession()
}

class SignInUserUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke(credentials: AuthCredentials) = repository.signIn(credentials)
}

class SignOutUserUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke() = repository.signOut()
}

class RefreshProfileEndpointUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke(endpoint: ProfileApiEndpoint, email: String) =
        repository.refreshProfileEndpoint(endpoint, email)
}

class GetLinkCandidatesUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke() = repository.getLinkCandidates()
}

class VerifyLinkedUserUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke(candidateId: String, token: String) =
        repository.verifyLinkedUser(candidateId, token)
}
