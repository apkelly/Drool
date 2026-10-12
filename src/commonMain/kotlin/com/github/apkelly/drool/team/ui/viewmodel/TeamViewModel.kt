package com.github.apkelly.drool.team.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.apkelly.drool.common.domain.model.OperationResult
import com.github.apkelly.drool.common.logging.DroolLog
import com.github.apkelly.drool.match.ui.viewmodel.MatchCache
import com.github.apkelly.drool.team.domain.usecase.LoadTeamHubUseCase
import com.github.apkelly.drool.team.ui.model.TeamHubUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TeamViewModel(
    private val loadTeamHub: LoadTeamHubUseCase,
    private val matchCache: MatchCache,
) : ViewModel() {
    private val logger = DroolLog.withTag("TeamViewModel")
    private val mutableState = MutableStateFlow<TeamHubUiState>(TeamHubUiState.Idle)
    private var ownerAccountId: String? = null
    val state: StateFlow<TeamHubUiState> = mutableState

    fun configureAccount(
        accountId: String,
    ) {
        if (ownerAccountId == accountId) return
        ownerAccountId = accountId
        matchCache.configureAccount(accountId)
        mutableState.value = TeamHubUiState.Idle
    }

    fun load(
        profileId: String,
        teamId: String,
        competitionName: String? = null,
        leagueId: String? = null,
    ) = request(profileId, teamId, competitionName, leagueId, force = false)

    fun refresh(
        profileId: String,
        teamId: String,
        competitionName: String? = null,
        leagueId: String? = null,
    ) = request(profileId, teamId, competitionName, leagueId, force = true)

    private fun request(
        profileId: String,
        teamId: String,
        competitionName: String?,
        leagueId: String?,
        force: Boolean,
    ) {
        val existing = (mutableState.value as? TeamHubUiState.Content)
            ?.takeIf { it.profileId == profileId && it.teamId == teamId }
        if (
            mutableState.value == TeamHubUiState.Loading(profileId, teamId) ||
            existing?.isRefreshing == true
        ) {
            return
        }
        val request: TeamHubUiState = if (force && existing != null) {
            existing.copy(isRefreshing = true, refreshFailure = null)
        } else {
            TeamHubUiState.Loading(profileId, teamId)
        }
        mutableState.value = request
        viewModelScope.launch {
            when (
                val result = loadTeamHub(
                    profileId,
                    teamId,
                    competitionName,
                    leagueId,
                    force,
                )
            ) {
                is OperationResult.Success -> {
                    val data = result.value
                    val hub = data.value
                    matchCache.putAll(profileId, hub.matches + hub.results)
                    if (mutableState.value == request) {
                        mutableState.value = TeamHubUiState.Content(
                            profileId = profileId,
                            teamId = teamId,
                            hub = hub,
                            lastUpdatedEpochMillis = data.lastUpdatedEpochMillis,
                            isStale = data.isStale,
                        )
                    }
                }

                is OperationResult.Failed -> {
                    logger.w { "Unable to load team hub: ${result.reason}" }
                    if (mutableState.value == request) {
                        mutableState.value = existing?.copy(
                            isRefreshing = false,
                            refreshFailure = result.reason,
                        ) ?: TeamHubUiState.Failed(profileId, teamId)
                    }
                }
            }
        }
    }
}
