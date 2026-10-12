package com.github.apkelly.drool.match.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.apkelly.drool.common.domain.model.OperationResult
import com.github.apkelly.drool.common.logging.DroolLog
import com.github.apkelly.drool.match.domain.usecase.LoadMatchDetailsUseCase
import com.github.apkelly.drool.match.domain.usecase.LoadMatchWeatherUseCase
import com.github.apkelly.drool.match.ui.model.MatchDetailsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MatchViewModel(
    private val loadMatchDetails: LoadMatchDetailsUseCase,
    private val loadMatchWeather: LoadMatchWeatherUseCase,
    private val matchCache: MatchCache,
) : ViewModel() {
    private val logger = DroolLog.withTag("MatchViewModel")
    private val mutableState = MutableStateFlow<MatchDetailsUiState>(MatchDetailsUiState.Idle)
    private var ownerAccountId: String? = null
    val state: StateFlow<MatchDetailsUiState> = mutableState

    fun configureAccount(
        accountId: String,
    ) {
        if (ownerAccountId == accountId) return
        ownerAccountId = accountId
        matchCache.configureAccount(accountId)
        mutableState.value = MatchDetailsUiState.Idle
    }

    fun load(
        profileId: String,
        matchId: String,
    ) = request(profileId, matchId, force = false)

    fun refresh(
        profileId: String,
        matchId: String,
    ) = request(profileId, matchId, force = true)

    private fun request(
        profileId: String,
        matchId: String,
        force: Boolean,
    ) {
        val key = MatchCacheKey(profileId, matchId)
        val current = (mutableState.value as? MatchDetailsUiState.Content)
            ?.takeIf { it.profileId == profileId && it.matchId == matchId }
        if (!force && matchCache.isResolved(key) && current != null) return
        if (force && current?.isRefreshing == true) return
        if (mutableState.value == MatchDetailsUiState.Loading(profileId, matchId)) return

        val cached = current?.fixture ?: matchCache.get(key)
        when {
            force && current != null -> mutableState.value = current.copy(
                isRefreshing = true,
                refreshFailed = false,
            )

            cached != null ->
                mutableState.value =
                    MatchDetailsUiState.Content(profileId, matchId, cached)

            else -> mutableState.value = MatchDetailsUiState.Loading(profileId, matchId)
        }

        viewModelScope.launch {
            var refreshFailed = false
            val fixture = when (val result = loadMatchDetails(profileId, matchId)) {
                is OperationResult.Success -> result.value.also {
                    matchCache.put(key, result.value, resolvedDetails = true)
                }

                is OperationResult.Failed -> {
                    logger.w { "Unable to load match details: ${result.reason}" }
                    if (cached == null) {
                        mutableState.value = MatchDetailsUiState.Failed(profileId, matchId)
                        return@launch
                    }
                    refreshFailed = true
                    cached
                }
            }
            mutableState.value = MatchDetailsUiState.Content(
                profileId = profileId,
                matchId = matchId,
                fixture = fixture,
                weather = current?.weather,
                isWeatherLoading = fixture.latitude != null && fixture.longitude != null,
                isRefreshing = force,
                refreshFailed = refreshFailed,
            )
            val weather = when (val result = loadMatchWeather(fixture, force)) {
                is OperationResult.Success -> result.value

                is OperationResult.Failed -> {
                    logger.w { "Unable to load match weather: ${result.reason}" }
                    refreshFailed = true
                    current?.weather
                }
            }
            val latest = mutableState.value as? MatchDetailsUiState.Content
            if (latest?.profileId == profileId && latest.matchId == matchId) {
                mutableState.value = latest.copy(
                    weather = weather,
                    isWeatherLoading = false,
                    weatherUnavailable = weather == null,
                    isRefreshing = false,
                    refreshFailed = refreshFailed,
                )
            }
        }
    }
}
