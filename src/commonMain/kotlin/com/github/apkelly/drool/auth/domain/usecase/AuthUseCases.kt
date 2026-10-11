package com.github.apkelly.drool.auth.domain.usecase

import com.github.apkelly.drool.auth.domain.repository.AuthRepository
import com.github.apkelly.drool.domain.model.AuthCredentials

class ObserveProfileUseCase(private val repository: AuthRepository) {
    operator fun invoke() = repository.observeProfile()
}

class RestoreSessionUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.restoreSession()
}

class SignInUserUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(credentials: AuthCredentials) = repository.signIn(credentials)
}

class SignOutUserUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.signOut()
}
