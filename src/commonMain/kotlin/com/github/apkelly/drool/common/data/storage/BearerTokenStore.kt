package com.github.apkelly.drool.common.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.apkelly.dribl.api.DriblTokenStore
import com.github.apkelly.drool.common.domain.model.AssignmentPollingInterval
import com.github.apkelly.drool.common.domain.model.ThemeMode
import com.github.apkelly.drool.match.domain.model.RefereeMatchSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath

internal const val DATA_STORE_FILE_NAME = "drool.preferences_pb"

class BearerTokenStore(
    private val dataStore: DataStore<Preferences>,
) : DriblTokenStore {
    fun observeBearerToken(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[BearerTokenKey]?.takeIf { it.isNotBlank() }
    }

    override suspend fun getBearerToken(): String? = dataStore.data.first()[BearerTokenKey]?.takeIf { it.isNotBlank() }

    override suspend fun saveBearerToken(
        token: String,
    ) {
        dataStore.edit { preferences ->
            preferences[BearerTokenKey] = token
        }
    }

    suspend fun saveSession(
        token: String,
        accountId: String,
    ) {
        dataStore.edit { preferences ->
            preferences[BearerTokenKey] = token
            preferences[ActiveAccountKey] = accountId
        }
    }

    suspend fun clearBearerToken() {
        dataStore.edit { preferences ->
            preferences.remove(BearerTokenKey)
            preferences.remove(ActiveAccountKey)
            preferences.remove(AssignmentBaselineAccountIdKey)
        }
    }

    suspend fun getActiveAccountId(): String? = dataStore.data.first()[ActiveAccountKey]?.takeIf { it.isNotBlank() }

    suspend fun setActiveAccountId(
        accountId: String,
    ) {
        dataStore.edit { preferences ->
            preferences[ActiveAccountKey] = accountId
        }
    }

    fun observeThemeMode(): Flow<ThemeMode> = dataStore.data.map { preferences ->
        preferences[ThemeModeKey]
            ?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
            ?: ThemeMode.System
    }

    suspend fun setThemeMode(
        mode: ThemeMode,
    ) {
        dataStore.edit { preferences ->
            preferences[ThemeModeKey] = mode.name
        }
    }

    fun observeObservabilityEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ObservabilityEnabledKey] ?: false
    }

    suspend fun setObservabilityEnabled(
        enabled: Boolean,
    ) {
        dataStore.edit { preferences ->
            preferences[ObservabilityEnabledKey] = enabled
        }
    }

    suspend fun getAssignmentBaselineAccountId(): String? = dataStore.data.first()[AssignmentBaselineAccountIdKey]

    suspend fun setAssignmentBaselineAccountId(
        accountId: String,
    ) {
        dataStore.edit { preferences ->
            preferences[AssignmentBaselineAccountIdKey] = accountId
        }
    }

    fun observeAssignmentPollingInterval(): Flow<AssignmentPollingInterval> = dataStore.data.map { preferences ->
        preferences[AssignmentPollingIntervalKey]
            ?.let { value ->
                AssignmentPollingInterval.entries.firstOrNull { it.name == value }
            }
            ?: AssignmentPollingInterval.Hourly
    }

    suspend fun setAssignmentPollingInterval(
        interval: AssignmentPollingInterval,
    ) {
        dataStore.edit { preferences ->
            preferences[AssignmentPollingIntervalKey] = interval.name
        }
    }

    suspend fun getRefereeMatchSession(
        profileId: String,
        matchId: String,
    ): RefereeMatchSession? {
        val raw = dataStore.data.first()[refereeMatchSessionKey(profileId, matchId)]
            ?.takeIf(String::isNotBlank)
            ?: return null
        return runCatching {
            sessionJson.decodeFromString<RefereeMatchSession>(raw)
        }.getOrNull()
    }

    suspend fun saveRefereeMatchSession(
        session: RefereeMatchSession,
    ) {
        val key = refereeMatchSessionKey(session.profileId, session.matchId)
        val encoded = sessionJson.encodeToString(session)
        dataStore.edit { preferences ->
            preferences[key] = encoded
        }
    }

    suspend fun clearRefereeMatchSession(
        profileId: String,
        matchId: String,
    ) {
        val key = refereeMatchSessionKey(profileId, matchId)
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    private companion object {
        val BearerTokenKey = stringPreferencesKey("bearer_token")
        val ActiveAccountKey = stringPreferencesKey("active_account_id")
        val ThemeModeKey = stringPreferencesKey("theme_mode")
        val ObservabilityEnabledKey = booleanPreferencesKey("observability_enabled")
        val AssignmentBaselineAccountIdKey =
            stringPreferencesKey("assignment_baseline_account_id")
        val AssignmentPollingIntervalKey =
            stringPreferencesKey("assignment_polling_interval")
        val sessionJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun refereeMatchSessionKey(
            profileId: String,
            matchId: String,
        ) = stringPreferencesKey("referee_match_session_${profileId}_$matchId")
    }
}

fun createBearerTokenStore(
    producePath: () -> String,
): BearerTokenStore = BearerTokenStore(
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() },
    ),
)

expect fun createPlatformBearerTokenStore(): BearerTokenStore
