package com.github.apkelly.drool.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.apkelly.drool.common.domain.model.CachedData
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.ProfileClub
import com.github.apkelly.drool.common.domain.model.ProfileTeam
import com.github.apkelly.drool.common.domain.model.RefereeAllocationDecision
import com.github.apkelly.drool.common.domain.model.RefreshFailure
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.logging.DroolLog
import com.github.apkelly.drool.common.observability.NoOpObservability
import com.github.apkelly.drool.common.observability.Observability
import com.github.apkelly.drool.common.observability.RefreshArea
import com.github.apkelly.drool.common.ui.model.CollectionUiState
import com.github.apkelly.drool.common.ui.viewmodel.collectionState
import com.github.apkelly.drool.common.ui.viewmodel.trackRefresh
import com.github.apkelly.drool.home.domain.usecase.ObserveProfileClubsUseCase
import com.github.apkelly.drool.home.domain.usecase.ObserveProfileTeamsUseCase
import com.github.apkelly.drool.schedule.domain.usecase.ObserveFixturesUseCase
import com.github.apkelly.drool.schedule.domain.usecase.RefreshFixturesUseCase
import com.github.apkelly.drool.schedule.domain.usecase.RefreshLinkedProfilesUseCase
import com.github.apkelly.drool.schedule.domain.usecase.UpdateRefereeAllocationUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    private val observeFixtures: ObserveFixturesUseCase,
    private val observeProfileTeams: ObserveProfileTeamsUseCase,
    private val observeProfileClubs: ObserveProfileClubsUseCase,
    private val refreshFixturesUseCase: RefreshFixturesUseCase,
    private val refreshLinkedProfiles: RefreshLinkedProfilesUseCase,
    private val updateRefereeAllocationUseCase: UpdateRefereeAllocationUseCase,
    private val observability: Observability = NoOpObservability,
) : ViewModel() {
    private val logger = DroolLog.withTag("ScheduleViewModel")
    private val fixturesRefreshing = MutableStateFlow(false)
    private val fixturesFailure = MutableStateFlow<RefreshFailure?>(null)
    private val fixturesRefreshMutex = Mutex()
    private val mutableAllocationUpdatingId = MutableStateFlow<String?>(null)
    private val mutableAllocationFailure = MutableStateFlow<RefreshFailure?>(null)
    private val mutableLinkedProfiles = MutableStateFlow<List<LinkedProfile>>(emptyList())
    private val mutableSelectedProfileId = MutableStateFlow<String?>(null)
    private val mutableLinkedProfileRefreshResults =
        MutableStateFlow<Map<String, RefreshResult>>(emptyMap())

    val linkedProfiles: StateFlow<List<LinkedProfile>> = mutableLinkedProfiles
    val selectedProfileId: StateFlow<String?> = mutableSelectedProfileId
    val linkedProfileRefreshResults: StateFlow<Map<String, RefreshResult>> =
        mutableLinkedProfileRefreshResults
    val allocationUpdatingId: StateFlow<String?> = mutableAllocationUpdatingId
    val allocationFailure: StateFlow<RefreshFailure?> = mutableAllocationFailure

    val fixtures: StateFlow<CollectionUiState<Fixture>> =
        combine(mutableLinkedProfiles, mutableSelectedProfileId) { profiles, selected ->
            profiles to selected
        }.flatMapLatest { (profiles, selected) ->
            if (profiles.isEmpty()) {
                observeFixtures()
            } else {
                observeFixtures(profiles.map { it.id }.toSet()).map { fixtures ->
                    CachedData(
                        value = fixtures.filter {
                            selected == null || it.profileId == selected
                        },
                        lastUpdatedEpochMillis = null,
                        isStale = fixtures.isEmpty(),
                    )
                }
            }
        }.combine(fixturesRefreshing) { data, refreshing -> data to refreshing }
            .combine(fixturesFailure) { (data, refreshing), failure ->
                collectionState(data, refreshing, failure)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CollectionUiState())

    val profileTeams: StateFlow<List<ProfileTeam>> =
        combine(mutableLinkedProfiles, mutableSelectedProfileId) { profiles, selected ->
            profiles to selected
        }.flatMapLatest { (profiles, selected) ->
            if (profiles.isEmpty()) {
                flowOf(emptyList())
            } else {
                observeProfileTeams(profiles.map { it.id }.toSet()).map { teams ->
                    teams.filter { selected == null || it.profileId == selected }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val profileClubs: StateFlow<List<ProfileClub>> =
        combine(mutableLinkedProfiles, mutableSelectedProfileId) { profiles, selected ->
            profiles to selected
        }.flatMapLatest { (profiles, selected) ->
            if (profiles.isEmpty()) {
                flowOf(emptyList())
            } else {
                observeProfileClubs(profiles.map { it.id }.toSet()).map { clubs ->
                    clubs.filter { selected == null || it.profileId == selected }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun configureLinkedProfiles(
        profile: Profile,
    ) {
        val profiles = profile.linkedProfiles
        if (mutableLinkedProfiles.value != profiles) {
            mutableLinkedProfiles.value = profiles
            mutableLinkedProfileRefreshResults.value = emptyMap()
            if (mutableSelectedProfileId.value !in profiles.map { it.id }) {
                mutableSelectedProfileId.value = null
            }
        }
    }

    fun selectLinkedProfile(
        profileId: String?,
    ) {
        mutableSelectedProfileId.value = profileId
    }

    fun refreshFixtures(
        force: Boolean = true,
    ) {
        viewModelScope.launch {
            fixturesRefreshMutex.withLock {
                fixturesRefreshing.value = true
                fixturesFailure.value = null
                try {
                    val profiles = mutableLinkedProfiles.value
                    val result = if (profiles.isEmpty()) {
                        refreshFixturesUseCase(force)
                    } else {
                        val resultsByProfile = refreshLinkedProfiles(profiles, force)
                        mutableLinkedProfileRefreshResults.value = resultsByProfile
                        val results = resultsByProfile.values
                        results.filterIsInstance<RefreshResult.Failed>().firstOrNull()
                            ?: if (results.all { it == RefreshResult.NotModified }) {
                                RefreshResult.NotModified
                            } else {
                                RefreshResult.Updated
                            }
                    }
                    observability.trackRefresh(RefreshArea.Fixtures, result)
                    if (result is RefreshResult.Failed) {
                        logger.w { "Refresh failed: ${result.reason}" }
                        fixturesFailure.value = result.reason
                    }
                } finally {
                    fixturesRefreshing.value = false
                }
            }
        }
    }

    fun updateRefereeAllocation(
        fixture: Fixture,
        decision: RefereeAllocationDecision,
    ) {
        val allocationId = fixture.refereeAllocationId ?: return
        val profile = mutableLinkedProfiles.value.firstOrNull { it.id == fixture.profileId }
            ?: return
        if (mutableAllocationUpdatingId.value != null) return

        viewModelScope.launch {
            mutableAllocationUpdatingId.value = allocationId
            mutableAllocationFailure.value = null
            try {
                when (
                    val result = updateRefereeAllocationUseCase(
                        profile = profile,
                        allocationId = allocationId,
                        decision = decision,
                    )
                ) {
                    is RefreshResult.Failed -> mutableAllocationFailure.value = result.reason

                    RefreshResult.NotModified,
                    RefreshResult.Updated,
                    -> refreshFixtures(force = true)
                }
            } finally {
                mutableAllocationUpdatingId.value = null
            }
        }
    }
}
