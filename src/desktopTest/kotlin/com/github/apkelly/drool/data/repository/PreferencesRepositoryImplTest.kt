package com.github.apkelly.drool.data.repository

import com.github.apkelly.drool.domain.model.ThemeMode
import com.github.apkelly.drool.profile.data.PreferencesRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PreferencesRepositoryImplTest {
    @Test
    fun readsAndWritesThemeMode() = runTest {
        TestTokenStore().use { tokenStore ->
            val repository = PreferencesRepositoryImpl(tokenStore.store)
            assertEquals(ThemeMode.System, repository.observeThemeMode().first())
            repository.setThemeMode(ThemeMode.Dark)
            assertEquals(ThemeMode.Dark, repository.observeThemeMode().first())
            assertEquals(false, repository.observeObservabilityEnabled().first())
            repository.setObservabilityEnabled(true)
            assertEquals(true, repository.observeObservabilityEnabled().first())
        }
    }
}
