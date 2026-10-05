package com.github.apkelly.drool.domain.usecase

import com.github.apkelly.drool.domain.model.ThemeMode
import com.github.apkelly.drool.domain.repository.PreferencesRepository

class ObserveThemeModeUseCase(private val repository: PreferencesRepository) {
    operator fun invoke() = repository.observeThemeMode()
}

class SetThemeModeUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class ObserveObservabilityEnabledUseCase(private val repository: PreferencesRepository) {
    operator fun invoke() = repository.observeObservabilityEnabled()
}

class SetObservabilityEnabledUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean) =
        repository.setObservabilityEnabled(enabled)
}
