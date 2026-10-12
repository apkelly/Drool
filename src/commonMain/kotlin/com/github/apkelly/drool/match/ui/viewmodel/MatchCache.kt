package com.github.apkelly.drool.match.ui.viewmodel

import com.github.apkelly.drool.common.domain.model.Fixture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal data class MatchCacheKey(
    val profileId: String,
    val matchId: String,
)

class MatchCache {
    private val entries = MutableStateFlow<Map<MatchCacheKey, Fixture>>(emptyMap())
    private val resolved = MutableStateFlow<Set<MatchCacheKey>>(emptySet())
    private var ownerAccountId: String? = null

    fun configureAccount(
        accountId: String,
    ) {
        if (ownerAccountId == accountId) return
        ownerAccountId = accountId
        entries.value = emptyMap()
        resolved.value = emptySet()
    }

    internal fun get(
        key: MatchCacheKey,
    ): Fixture? = entries.value[key]

    internal fun isResolved(
        key: MatchCacheKey,
    ): Boolean = key in resolved.value

    internal fun put(
        key: MatchCacheKey,
        fixture: Fixture,
        resolvedDetails: Boolean = false,
    ) {
        entries.update { cached ->
            LinkedHashMap(cached).apply {
                remove(key)
                put(key, fixture)
                while (size > MAXIMUM_SIZE) remove(keys.first())
            }
        }
        if (resolvedDetails) resolved.update { it + key }
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
