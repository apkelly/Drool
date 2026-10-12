package com.github.apkelly.drool.match.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.apkelly.drool.common.domain.model.OperationResult
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
import com.github.apkelly.drool.common.logging.DroolLog
import com.github.apkelly.drool.match.domain.usecase.LoadMatchDetailsUseCase
import com.github.apkelly.drool.match.domain.usecase.LoadMatchSheetUseCase
import com.github.apkelly.drool.match.domain.usecase.LoadMatchWeatherUseCase
import com.github.apkelly.drool.match.domain.usecase.UpdateMatchSheetMemberUseCase
import com.github.apkelly.drool.match.domain.usecase.UpdateTeamSheetStatusUseCase
import com.github.apkelly.drool.match.ui.model.MatchDetailsUiState
import com.github.apkelly.drool.match.ui.model.MatchSheetUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MatchViewModel(
    private val loadMatchDetails: LoadMatchDetailsUseCase,
    private val loadMatchWeather: LoadMatchWeatherUseCase,
    private val loadMatchSheetUseCase: LoadMatchSheetUseCase,
    private val updateMatchSheetMemberUseCase: UpdateMatchSheetMemberUseCase,
    private val updateTeamSheetStatusUseCase: UpdateTeamSheetStatusUseCase,
    private val matchCache: MatchCache,
) : ViewModel() {
    private val logger = DroolLog.withTag("MatchViewModel")
    private val mutableState = MutableStateFlow<MatchDetailsUiState>(MatchDetailsUiState.Idle)
    private val mutableMatchSheetState =
        MutableStateFlow<MatchSheetUiState>(MatchSheetUiState.Idle)
    private var ownerAccountId: String? = null
    val state: StateFlow<MatchDetailsUiState> = mutableState
    val matchSheetState: StateFlow<MatchSheetUiState> = mutableMatchSheetState

    fun configureAccount(
        accountId: String,
    ) {
        if (ownerAccountId == accountId) return
        ownerAccountId = accountId
        matchCache.configureAccount(accountId)
        mutableState.value = MatchDetailsUiState.Idle
        mutableMatchSheetState.value = MatchSheetUiState.Idle
    }

    fun load(
        profileId: String,
        matchId: String,
    ) = request(profileId, matchId, force = false)

    fun refresh(
        profileId: String,
        matchId: String,
    ) = request(profileId, matchId, force = true)

    fun loadMatchSheet(
        profileId: String,
        force: Boolean = false,
    ) {
        val details = mutableState.value as? MatchDetailsUiState.Content ?: return
        val current = mutableMatchSheetState.value as? MatchSheetUiState.Content
        if (!force && current?.profileId == profileId && current.matchId == details.matchId) return
        mutableMatchSheetState.value = MatchSheetUiState.Loading(profileId, details.matchId)
        viewModelScope.launch {
            mutableMatchSheetState.value = when (
                val result = loadMatchSheetUseCase(profileId, details.fixture)
            ) {
                is OperationResult.Success -> MatchSheetUiState.Content(
                    profileId = profileId,
                    matchId = details.matchId,
                    matchSheet = result.value,
                )

                is OperationResult.Failed -> {
                    logger.w { "Unable to load match sheet: ${result.reason}" }
                    MatchSheetUiState.Failed(profileId, details.matchId)
                }
            }
        }
    }

    fun updateMemberAvailability(
        profileId: String,
        memberId: String,
        available: Boolean,
    ) {
        val current = mutableMatchSheetState.value as? MatchSheetUiState.Content ?: return
        if (current.updatingMemberId != null || current.updatingTeamId != null) return
        mutableMatchSheetState.value = current.copy(
            updatingMemberId = memberId,
            updateFailed = false,
        )
        viewModelScope.launch {
            when (val result = updateMatchSheetMemberUseCase(profileId, memberId, available)) {
                is OperationResult.Success -> {
                    val latest = mutableMatchSheetState.value as? MatchSheetUiState.Content
                        ?: return@launch
                    mutableMatchSheetState.value = latest.copy(
                        matchSheet = latest.matchSheet.copy(
                            members = latest.matchSheet.members.map { member ->
                                if (member.id == memberId) result.value else member
                            },
                        ),
                        updatingMemberId = null,
                    )
                }

                is OperationResult.Failed -> {
                    logger.w { "Unable to update match sheet member: ${result.reason}" }
                    mutableMatchSheetState.value = current.copy(updateFailed = true)
                }
            }
        }
    }

    fun updateTeamStatus(
        profileId: String,
        teamId: String,
        action: TeamSheetAction,
    ) {
        val current = mutableMatchSheetState.value as? MatchSheetUiState.Content ?: return
        if (current.updatingMemberId != null || current.updatingTeamId != null) return
        mutableMatchSheetState.value = current.copy(
            updatingTeamId = teamId,
            updateFailed = false,
        )
        viewModelScope.launch {
            when (
                val result = updateTeamSheetStatusUseCase(
                    profileId = profileId,
                    matchSheetId = current.matchSheet.id,
                    teamId = teamId,
                    action = action,
                )
            ) {
                is OperationResult.Success -> {
                    val latest = mutableMatchSheetState.value as? MatchSheetUiState.Content
                        ?: return@launch
                    val sheet = latest.matchSheet
                    mutableMatchSheetState.value = latest.copy(
                        matchSheet = sheet.copy(
                            homeTeam = sheet.homeTeam.takeUnless { it.id == teamId }
                                ?: sheet.homeTeam.copy(status = result.value),
                            awayTeam = sheet.awayTeam.takeUnless { it.id == teamId }
                                ?: sheet.awayTeam.copy(status = result.value),
                        ),
                        updatingTeamId = null,
                    )
                }

                is OperationResult.Failed -> {
                    logger.w { "Unable to update team sheet status: ${result.reason}" }
                    mutableMatchSheetState.value = current.copy(updateFailed = true)
                }
            }
        }
    }

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
