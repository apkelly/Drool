package com.github.apkelly.drool.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.github.apkelly.drool.domain.model.ThemeMode
import okio.Path.Companion.toPath

internal const val DataStoreFileName = "drool.preferences_pb"

class BearerTokenStore(private val dataStore: DataStore<Preferences>) {
    fun observeBearerToken(): Flow<String?> =
        dataStore.data.map { preferences ->
            preferences[BearerTokenKey]?.takeIf { it.isNotBlank() }
        }

    suspend fun getBearerToken(): String? =
        dataStore.data.first()[BearerTokenKey]?.takeIf { it.isNotBlank() }

    suspend fun saveBearerToken(token: String) {
        dataStore.edit { preferences ->
            preferences[BearerTokenKey] = token
        }
    }

    suspend fun clearBearerToken() {
        dataStore.edit { preferences ->
            preferences.remove(BearerTokenKey)
            preferences.remove(ActiveAccountKey)
        }
    }

    suspend fun getActiveAccountId(): String? =
        dataStore.data.first()[ActiveAccountKey]?.takeIf { it.isNotBlank() }

    suspend fun setActiveAccountId(accountId: String) {
        dataStore.edit { preferences ->
            preferences[ActiveAccountKey] = accountId
        }
    }

    fun observeThemeMode(): Flow<ThemeMode> =
        dataStore.data.map { preferences ->
            preferences[ThemeModeKey]
                ?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
                ?: ThemeMode.System
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[ThemeModeKey] = mode.name
        }
    }

    private companion object {
        val BearerTokenKey = stringPreferencesKey("bearer_token")
        val ActiveAccountKey = stringPreferencesKey("active_account_id")
        val ThemeModeKey = stringPreferencesKey("theme_mode")
    }
}

fun createBearerTokenStore(producePath: () -> String): BearerTokenStore =
    BearerTokenStore(
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { producePath().toPath() },
        ),
    )

expect fun createPlatformBearerTokenStore(): BearerTokenStore
