package com.github.apkelly.drool.domain.repository

import kotlinx.coroutines.flow.Flow
import com.github.apkelly.drool.domain.model.ThemeMode

interface PreferencesRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
