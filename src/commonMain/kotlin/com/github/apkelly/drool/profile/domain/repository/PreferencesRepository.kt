package com.github.apkelly.drool.profile.domain.repository

import com.github.apkelly.drool.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(
        mode: ThemeMode,
    )
    fun observeObservabilityEnabled(): Flow<Boolean>
    suspend fun setObservabilityEnabled(
        enabled: Boolean,
    )
}
