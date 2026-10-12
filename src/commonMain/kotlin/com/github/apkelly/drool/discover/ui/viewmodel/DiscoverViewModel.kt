package com.github.apkelly.drool.discover.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.RefreshFailure
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.domain.model.Team
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import com.github.apkelly.drool.common.logging.DroolLog
import com.github.apkelly.drool.common.observability.NoOpObservability
import com.github.apkelly.drool.common.observability.Observability
import com.github.apkelly.drool.common.observability.RefreshArea
import com.github.apkelly.drool.common.ui.model.CollectionUiState
import com.github.apkelly.drool.common.ui.viewmodel.collectionState
import com.github.apkelly.drool.common.ui.viewmodel.trackRefresh
import com.github.apkelly.drool.discover.domain.usecase.ObserveClubsUseCase
import com.github.apkelly.drool.discover.domain.usecase.ObserveTeamRelationshipsUseCase
import com.github.apkelly.drool.discover.domain.usecase.ObserveTeamsUseCase
import com.github.apkelly.drool.discover.domain.usecase.RefreshClubsUseCase
import com.github.apkelly.drool.discover.domain.usecase.RefreshTeamsUseCase
import com.github.apkelly.drool.discover.domain.usecase.SetTeamFollowingUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModel(
    observeClubs: ObserveClubsUseCase,
    private val observeTeams: ObserveTeamsUseCase,
    observeRelationships: ObserveTeamRelationshipsUseCase,
    private val refreshClubsUseCase: RefreshClubsUseCase,
    private val refreshTeamsUseCase: RefreshTeamsUseCase,
    private val setTeamFollowing: SetTeamFollowingUseCase,
    private val observability: Observability = NoOpObservability,
) : ViewModel() {
    private val logger = DroolLog.withTag("DiscoverViewModel")
    private val clubsRefreshing = MutableStateFlow(false)
    private val teamsRefreshing = MutableStateFlow(false)
    private val clubsFailure = MutableStateFlow<RefreshFailure?>(null)
    private val teamsFailure = MutableStateFlow<RefreshFailure?>(null)
    private val selectedClubId = MutableStateFlow<String?>(null)
    private val teamsRefreshMutex = Mutex()

    val clubs: StateFlow<CollectionUiState<Club>> =
        combine(observeClubs(), clubsRefreshing, clubsFailure, ::collectionState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, CollectionUiState())

    val teams: StateFlow<CollectionUiState<Team>> =
        selectedClubId
            .flatMapLatest(observeTeams::invoke)
            .combine(teamsRefreshing) { data, refreshing -> data to refreshing }
            .combine(teamsFailure) { (data, refreshing), failure ->
                collectionState(data, refreshing, failure)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CollectionUiState())

    val relationships: StateFlow<Map<String, TeamRelationship>> =
        observeRelationships()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun selectClub(
        clubId: String?,
    ) {
        val changed = selectedClubId.value != clubId
        selectedClubId.value = clubId
        if (changed && clubId != null) refreshTeams(force = false)
    }

    fun refreshClubs(
        force: Boolean = true,
    ) {
        if (clubsRefreshing.value) return
        viewModelScope.launch {
            clubsRefreshing.value = true
            clubsFailure.value = null
            val result = refreshClubsUseCase(force)
            observability.trackRefresh(RefreshArea.Clubs, result)
            if (result is RefreshResult.Failed) {
                logger.w { "Refresh failed: ${result.reason}" }
                clubsFailure.value = result.reason
            }
            clubsRefreshing.value = false
        }
    }

    fun refreshTeams(
        force: Boolean = true,
    ) {
        val clubId = selectedClubId.value ?: return
        viewModelScope.launch {
            teamsRefreshMutex.withLock {
                teamsRefreshing.value = true
                teamsFailure.value = null
                try {
                    val result = refreshTeamsUseCase(force, clubId)
                    observability.trackRefresh(RefreshArea.Teams, result)
                    if (result is RefreshResult.Failed) {
                        logger.w { "Refresh failed: ${result.reason}" }
                        teamsFailure.value = result.reason
                    }
                } finally {
                    teamsRefreshing.value = false
                }
            }
        }
    }

    fun setFollowing(
        teamId: String,
        following: Boolean,
    ) {
        viewModelScope.launch { setTeamFollowing(teamId, following) }
    }
}
