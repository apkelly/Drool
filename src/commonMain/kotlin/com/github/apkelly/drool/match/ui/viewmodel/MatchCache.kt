package com.github.apkelly.drool.match.ui.viewmodel

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.match.domain.model.RefereeMatchSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal data class MatchCacheKey(
    val profileId: String,
    val matchId: String,
)

class MatchCache {
    private val entries = MutableStateFlow<Map<MatchCacheKey, Fixture>>(emptyMap())
    private val resolved = MutableStateFlow<Set<MatchCacheKey>>(emptySet())
    private val refereeSessions =
        MutableStateFlow<Map<MatchCacheKey, RefereeMatchSession>>(emptyMap())
    private var ownerAccountId: String? = null

    fun configureAccount(
        accountId: String,
    ) {
        val previousAccountId = ownerAccountId
        ownerAccountId = accountId
        if (previousAccountId != null && previousAccountId != accountId) {
            entries.value = emptyMap()
            resolved.value = emptySet()
            refereeSessions.value = emptyMap()
        }
    }

    fun clear() {
        ownerAccountId = null
        entries.value = emptyMap()
        resolved.value = emptySet()
        refereeSessions.value = emptyMap()
    }

    internal fun get(
        key: MatchCacheKey,
    ): Fixture? = entries.value[key]

    internal fun isResolved(
        key: MatchCacheKey,
    ): Boolean = key in resolved.value

    internal fun getRefereeSession(
        key: MatchCacheKey,
    ): RefereeMatchSession? = refereeSessions.value[key]

    internal fun putRefereeSession(
        key: MatchCacheKey,
        session: RefereeMatchSession,
    ) {
        refereeSessions.update { cached ->
            LinkedHashMap(cached).apply {
                remove(key)
                put(key, session)
                while (size > MAXIMUM_SIZE) {
                    remove(keys.first())
                }
            }
        }
    }

    internal fun put(
        key: MatchCacheKey,
        fixture: Fixture,
        resolvedDetails: Boolean = false,
    ) {
        val evictedKeys = mutableSetOf<MatchCacheKey>()
        entries.update { cached ->
            LinkedHashMap(cached).apply {
                remove(key)
                put(key, fixture)
                while (size > MAXIMUM_SIZE) {
                    val evicted = keys.first()
                    remove(evicted)
                    evictedKeys += evicted
                }
            }
        }
        resolved.update { current ->
            val cleaned = if (evictedKeys.isEmpty()) current else current - evictedKeys
            if (resolvedDetails) cleaned + key else cleaned
        }
    }

    fun putAll(
        profileId: String,
        fixtures: List<Fixture>,
    ) {
        fixtures.forEach { fixture ->
            put(MatchCacheKey(profileId, fixture.id), fixture)
        }
    }

    private companion object {
        const val MAXIMUM_SIZE = 256
    }
}
