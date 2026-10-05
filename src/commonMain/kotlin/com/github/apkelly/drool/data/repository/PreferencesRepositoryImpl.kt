package com.github.apkelly.drool.data.repository

import com.github.apkelly.drool.data.storage.BearerTokenStore
import com.github.apkelly.drool.domain.model.ThemeMode
import com.github.apkelly.drool.domain.repository.PreferencesRepository

class PreferencesRepositoryImpl(
    private val store: BearerTokenStore,
) : PreferencesRepository {
    override fun observeThemeMode() = store.observeThemeMode()

    override suspend fun setThemeMode(mode: ThemeMode) =
        store.setThemeMode(mode)

    override fun observeObservabilityEnabled() = store.observeObservabilityEnabled()

    override suspend fun setObservabilityEnabled(enabled: Boolean) =
        store.setObservabilityEnabled(enabled)
}
