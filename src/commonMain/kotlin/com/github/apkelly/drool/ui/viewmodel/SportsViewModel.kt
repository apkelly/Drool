package com.github.apkelly.drool.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.github.apkelly.drool.domain.model.Club
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FamilyProfile
import com.github.apkelly.drool.domain.model.FamilyTeam
import com.github.apkelly.drool.domain.model.FamilyClub
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.RefreshFailure
import com.github.apkelly.drool.domain.model.RefreshResult
import com.github.apkelly.drool.domain.model.Team
import com.github.apkelly.drool.domain.model.TeamHub
import com.github.apkelly.drool.domain.model.TeamRelationship
import com.github.apkelly.drool.domain.usecase.ObserveClubsUseCase
import com.github.apkelly.drool.domain.usecase.ObserveFixturesUseCase
import com.github.apkelly.drool.domain.usecase.ObserveFamilyTeamsUseCase
import com.github.apkelly.drool.domain.usecase.ObserveFamilyClubsUseCase
import com.github.apkelly.drool.domain.usecase.ObserveTeamRelationshipsUseCase
import com.github.apkelly.drool.domain.usecase.ObserveTeamsUseCase
import com.github.apkelly.drool.domain.usecase.RefreshClubsUseCase
import com.github.apkelly.drool.domain.usecase.RefreshFixturesUseCase
import com.github.apkelly.drool.domain.usecase.RefreshFamilyProfilesUseCase
import com.github.apkelly.drool.domain.usecase.RefreshTeamsUseCase
import com.github.apkelly.drool.domain.usecase.SetTeamFollowingUseCase
import com.github.apkelly.drool.domain.usecase.LoadTeamHubUseCase
import com.github.apkelly.drool.domain.usecase.LoadMatchDetailsUseCase
import com.github.apkelly.drool.logging.DroolLog
import com.github.apkelly.drool.ui.model.CollectionUiState
import com.github.apkelly.drool.ui.model.TeamHubUiState
import com.github.apkelly.drool.ui.model.MatchDetailsUiState

private data class TeamHubCacheKey(val profileId: String, val teamId: String)
private data class MatchCacheKey(val profileId: String, val matchId: String)

private fun <K, V> Map<K, V>.withCachedEntries(
    entries: Map<K, V>,
    maximumSize: Int,
): Map<K, V> {
    val updated = LinkedHashMap(this)
    entries.forEach { (key, value) ->
        updated.remove(key)
        updated[key] = value
    }
    while (updated.size > maximumSize) {
        updated.remove(updated.keys.first())
    }
    return updated
}

@OptIn(ExperimentalCoroutinesApi::class)
class SportsViewModel(
    observeClubs: ObserveClubsUseCase,
    private val observeTeams: ObserveTeamsUseCase,
    private val observeFixtures: ObserveFixturesUseCase,
    private val observeFamilyTeams: ObserveFamilyTeamsUseCase,
    private val observeFamilyClubs: ObserveFamilyClubsUseCase,
    observeRelationships: ObserveTeamRelationshipsUseCase,
    private val refreshClubsUseCase: RefreshClubsUseCase,
    private val refreshTeamsUseCase: RefreshTeamsUseCase,
    private val refreshFixturesUseCase: RefreshFixturesUseCase,
    private val refreshFamilyProfiles: RefreshFamilyProfilesUseCase,
    private val setTeamFollowing: SetTeamFollowingUseCase,
    private val loadTeamHub: LoadTeamHubUseCase,
    private val loadMatchDetails: LoadMatchDetailsUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val logger = DroolLog.withTag("SportsViewModel")
    private val clubsRefreshing = MutableStateFlow(false)
    private val teamsRefreshing = MutableStateFlow(false)
    private val fixturesRefreshing = MutableStateFlow(false)
    private val teamsRefreshMutex = Mutex()
    private val fixturesRefreshMutex = Mutex()
    private val clubsFailure = MutableStateFlow<RefreshFailure?>(null)
    private val teamsFailure = MutableStateFlow<RefreshFailure?>(null)
    private val fixturesFailure = MutableStateFlow<RefreshFailure?>(null)
    private val selectedClubId = MutableStateFlow<String?>(null)
    private val mutableFamilyProfiles = MutableStateFlow<List<FamilyProfile>>(emptyList())
    private val mutableSelectedProfileId = MutableStateFlow<String?>(null)
    private val mutableFamilyRefreshResults =
        MutableStateFlow<Map<String, RefreshResult>>(emptyMap())
    private val mutableTeamHubState = MutableStateFlow<TeamHubUiState>(TeamHubUiState.Idle)
    private val mutableMatchDetailsState =
        MutableStateFlow<MatchDetailsUiState>(MatchDetailsUiState.Idle)
    private val teamHubCache = MutableStateFlow<Map<TeamHubCacheKey, TeamHub>>(emptyMap())
    private val matchCache = MutableStateFlow<Map<MatchCacheKey, Fixture>>(emptyMap())
    private var cacheOwnerAccountId: String? = null

    val familyProfiles: StateFlow<List<FamilyProfile>> = mutableFamilyProfiles
    val selectedProfileId: StateFlow<String?> = mutableSelectedProfileId
    val familyRefreshResults: StateFlow<Map<String, RefreshResult>> =
        mutableFamilyRefreshResults
    val teamHubState: StateFlow<TeamHubUiState> = mutableTeamHubState
    val matchDetailsState: StateFlow<MatchDetailsUiState> = mutableMatchDetailsState

    val clubs: StateFlow<CollectionUiState<Club>> =
        combine(observeClubs(), clubsRefreshing, clubsFailure, ::collectionState)
            .stateIn(scope, SharingStarted.Eagerly, CollectionUiState())

    val teams: StateFlow<CollectionUiState<Team>> =
        selectedClubId
            .flatMapLatest(observeTeams::invoke)
            .combine(teamsRefreshing) { data, refreshing ->
                data to refreshing
            }
            .combine(teamsFailure) { (data, refreshing), failure ->
                CollectionUiState(
                    items = data.value,
                    lastUpdatedEpochMillis = data.lastUpdatedEpochMillis,
                    isStale = data.isStale,
                    isRefreshing = refreshing,
                    refreshFailure = failure,
                )
            }
            .stateIn(scope, SharingStarted.Eagerly, CollectionUiState())

    val fixtures: StateFlow<CollectionUiState<Fixture>> =
        combine(mutableFamilyProfiles, mutableSelectedProfileId) { profiles, selected ->
            profiles to selected
        }.flatMapLatest { (profiles, selected) ->
            if (profiles.isEmpty()) {
                observeFixtures()
            } else {
                observeFixtures(profiles.map { it.id }.toSet()).map { fixtures ->
                    com.github.apkelly.drool.domain.model.CachedData(
                        value = fixtures.filter {
                            selected == null || it.profileId == selected
                        },
                        lastUpdatedEpochMillis = null,
                        isStale = fixtures.isEmpty(),
                    )
                }
            }
        }.combine(fixturesRefreshing) { data, refreshing ->
            data to refreshing
        }.combine(fixturesFailure) { (data, refreshing), failure ->
            collectionState(data, refreshing, failure)
        }
            .stateIn(scope, SharingStarted.Eagerly, CollectionUiState())

    val familyTeams: StateFlow<List<FamilyTeam>> =
        combine(mutableFamilyProfiles, mutableSelectedProfileId) { profiles, selected ->
            profiles to selected
        }.flatMapLatest { (profiles, selected) ->
            if (profiles.isEmpty()) {
                flowOf(emptyList())
            } else {
                observeFamilyTeams(profiles.map { it.id }.toSet()).map { teams ->
                    teams.filter { selected == null || it.profileId == selected }
                }
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val familyClubs: StateFlow<List<FamilyClub>> =
        combine(mutableFamilyProfiles, mutableSelectedProfileId) { profiles, selected ->
            profiles to selected
        }.flatMapLatest { (profiles, selected) ->
            if (profiles.isEmpty()) {
                flowOf(emptyList())
            } else {
                observeFamilyClubs(profiles.map { it.id }.toSet()).map { clubs ->
                    clubs.filter { selected == null || it.profileId == selected }
                }
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val relationships: StateFlow<Map<String, TeamRelationship>> =
        observeRelationships()
            .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    fun selectClub(clubId: String?) {
        val changed = selectedClubId.value != clubId
        selectedClubId.value = clubId
        if (changed && clubId != null) {
            refreshTeams(force = false)
        }
    }

    fun configureFamily(profile: Profile) {
        if (cacheOwnerAccountId != profile.accountId) {
            cacheOwnerAccountId = profile.accountId
            teamHubCache.value = emptyMap()
            matchCache.value = emptyMap()
            mutableTeamHubState.value = TeamHubUiState.Idle
            mutableMatchDetailsState.value = MatchDetailsUiState.Idle
        }
        val profiles = profile.familyProfiles
        if (mutableFamilyProfiles.value != profiles) {
            mutableFamilyProfiles.value = profiles
            mutableFamilyRefreshResults.value = emptyMap()
            if (mutableSelectedProfileId.value !in profiles.map { it.id }) {
                mutableSelectedProfileId.value = null
            }
        }
    }

    fun selectFamilyProfile(profileId: String?) {
        mutableSelectedProfileId.value = profileId
    }

    fun refreshAll(force: Boolean = false) {
        refreshClubs(force)
        refreshFixtures(force)
    }

    fun refreshClubs(force: Boolean = true) =
        refresh(clubsRefreshing, clubsFailure) { refreshClubsUseCase(force) }

    fun refreshTeams(force: Boolean = true) {
        val clubId = selectedClubId.value ?: return
        scope.launch {
            teamsRefreshMutex.withLock {
                teamsRefreshing.value = true
                teamsFailure.value = null
                try {
                    val result = refreshTeamsUseCase(force, clubId)
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

    fun refreshFixtures(force: Boolean = true) {
        scope.launch {
            fixturesRefreshMutex.withLock {
                fixturesRefreshing.value = true
                fixturesFailure.value = null
                try {
                    val profiles = mutableFamilyProfiles.value
                    val result = if (profiles.isEmpty()) {
                        refreshFixturesUseCase(force)
                    } else {
                        val resultsByProfile = refreshFamilyProfiles(profiles, force)
                        mutableFamilyRefreshResults.value = resultsByProfile
                        val results = resultsByProfile.values
                        results.filterIsInstance<RefreshResult.Failed>().firstOrNull()
                            ?: if (results.all { it == RefreshResult.NotModified }) {
                                RefreshResult.NotModified
                            } else {
                                RefreshResult.Updated
                            }
                    }
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

    fun setFollowing(teamId: String, following: Boolean) {
        scope.launch { setTeamFollowing(teamId, following) }
    }

    fun loadTeam(profileId: String, teamId: String) {
        val key = TeamHubCacheKey(profileId, teamId)
        teamHubCache.value[key]?.let { cached ->
            mutableTeamHubState.value =
                TeamHubUiState.Content(profileId, teamId, cached)
            return
        }
        if (mutableTeamHubState.value == TeamHubUiState.Loading(profileId, teamId)) {
            return
        }
        val request = TeamHubUiState.Loading(profileId, teamId)
        mutableTeamHubState.value = request
        scope.launch {
            try {
                val hub = loadTeamHub(profileId, teamId)
                teamHubCache.update {
                    it.withCachedEntries(mapOf(key to hub), TEAM_HUB_CACHE_SIZE)
                }
                matchCache.update { cached ->
                    cached.withCachedEntries(
                        entries = (hub.matches + hub.results).associateBy(
                            keySelector = { MatchCacheKey(profileId, it.id) },
                            valueTransform = { it },
                        ),
                        maximumSize = MATCH_CACHE_SIZE,
                    )
                }
                if (mutableTeamHubState.value == request) {
                    mutableTeamHubState.value = TeamHubUiState.Content(profileId, teamId, hub)
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.w { "Unable to load team hub (${error::class.simpleName})" }
                if (mutableTeamHubState.value == request) {
                    mutableTeamHubState.value = TeamHubUiState.Failed(profileId, teamId)
                }
            }
        }
    }

    fun loadMatch(profileId: String, matchId: String) {
        val key = MatchCacheKey(profileId, matchId)
        matchCache.value[key]?.let { cached ->
            mutableMatchDetailsState.value =
                MatchDetailsUiState.Content(profileId, matchId, cached)
            return
        }
        if (mutableMatchDetailsState.value == MatchDetailsUiState.Loading(profileId, matchId)) {
            return
        }
        val request = MatchDetailsUiState.Loading(profileId, matchId)
        mutableMatchDetailsState.value = request
        scope.launch {
            try {
                val fixture = loadMatchDetails(profileId, matchId)
                matchCache.update {
                    it.withCachedEntries(mapOf(key to fixture), MATCH_CACHE_SIZE)
                }
                if (mutableMatchDetailsState.value == request) {
                    mutableMatchDetailsState.value =
                        MatchDetailsUiState.Content(profileId, matchId, fixture)
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.w { "Unable to load match details (${error::class.simpleName})" }
                if (mutableMatchDetailsState.value == request) {
                    mutableMatchDetailsState.value =
                        MatchDetailsUiState.Failed(profileId, matchId)
                }
            }
        }
    }

    private fun refresh(
        refreshing: MutableStateFlow<Boolean>,
        failure: MutableStateFlow<RefreshFailure?>,
        block: suspend () -> RefreshResult,
    ) {
        if (refreshing.value) return
        scope.launch {
            refreshing.value = true
            failure.value = null
            val result = block()
            if (result is RefreshResult.Failed) {
                logger.w { "Refresh failed: ${result.reason}" }
                failure.value = result.reason
            }
            refreshing.value = false
        }
    }

    private companion object {
        const val TEAM_HUB_CACHE_SIZE = 12
        const val MATCH_CACHE_SIZE = 256

        fun <T> collectionState(
            data: com.github.apkelly.drool.domain.model.CachedData<List<T>>,
            refreshing: Boolean,
            failure: RefreshFailure?,
        ) = CollectionUiState(
            items = data.value,
            lastUpdatedEpochMillis = data.lastUpdatedEpochMillis,
            isStale = data.isStale,
            isRefreshing = refreshing,
            refreshFailure = failure,
        )
    }
}
