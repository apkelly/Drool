package com.github.apkelly.drool.profile.data

import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.domain.model.AssignmentPollingInterval
import com.github.apkelly.drool.common.domain.model.ThemeMode
import com.github.apkelly.drool.profile.domain.repository.PreferencesRepository

class PreferencesRepositoryImpl(
    private val store: BearerTokenStore,
) : PreferencesRepository {
    override fun observeThemeMode() = store.observeThemeMode()

    override suspend fun setThemeMode(
        mode: ThemeMode,
    ) = store.setThemeMode(mode)

    override fun observeObservabilityEnabled() = store.observeObservabilityEnabled()

    override suspend fun setObservabilityEnabled(
        enabled: Boolean,
    ) = store.setObservabilityEnabled(enabled)

    override fun observeAssignmentPollingInterval() = store.observeAssignmentPollingInterval()

    override suspend fun setAssignmentPollingInterval(
        interval: AssignmentPollingInterval,
    ) = store.setAssignmentPollingInterval(interval)
}
