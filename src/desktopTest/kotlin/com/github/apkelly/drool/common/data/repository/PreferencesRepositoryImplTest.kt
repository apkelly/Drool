package com.github.apkelly.drool.common.data.repository

import com.github.apkelly.drool.common.domain.model.AssignmentPollingInterval
import com.github.apkelly.drool.common.domain.model.ThemeMode
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
            assertEquals(
                AssignmentPollingInterval.Hourly,
                repository.observeAssignmentPollingInterval().first(),
            )
            repository.setAssignmentPollingInterval(AssignmentPollingInterval.EveryThreeHours)
            assertEquals(
                AssignmentPollingInterval.EveryThreeHours,
                repository.observeAssignmentPollingInterval().first(),
            )
        }
    }
}
