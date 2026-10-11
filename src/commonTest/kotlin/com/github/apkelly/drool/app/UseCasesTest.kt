package com.github.apkelly.drool.app

import com.github.apkelly.drool.app.ui.viewmodel.SportsViewModel
import com.github.apkelly.drool.auth.data.AuthRepositoryImpl
import com.github.apkelly.drool.auth.domain.usecase.ObserveProfileUseCase
import com.github.apkelly.drool.auth.domain.usecase.RestoreSessionUseCase
import com.github.apkelly.drool.auth.domain.usecase.SignInUserUseCase
import com.github.apkelly.drool.auth.domain.usecase.SignOutUserUseCase
import com.github.apkelly.drool.common.data.repository.SessionDataSource
import com.github.apkelly.drool.common.data.repository.SportsDataSource
import com.github.apkelly.drool.common.domain.model.AuthCredentials
import com.github.apkelly.drool.common.domain.model.AuthenticatedSession
import com.github.apkelly.drool.common.domain.model.CachedData
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.FamilyClub
import com.github.apkelly.drool.common.domain.model.FamilyProfile
import com.github.apkelly.drool.common.domain.model.FamilyTeam
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import com.github.apkelly.drool.common.domain.model.LinkMemberRequestResult
import com.github.apkelly.drool.common.domain.model.MatchWeather
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.Team
import com.github.apkelly.drool.common.domain.model.TeamHub
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import com.github.apkelly.drool.common.domain.model.ThemeMode
import com.github.apkelly.drool.discover.data.DiscoverRepositoryImpl
import com.github.apkelly.drool.discover.domain.usecase.ObserveClubsUseCase
import com.github.apkelly.drool.discover.domain.usecase.ObserveTeamRelationshipsUseCase
import com.github.apkelly.drool.discover.domain.usecase.ObserveTeamsUseCase
import com.github.apkelly.drool.discover.domain.usecase.RefreshClubsUseCase
import com.github.apkelly.drool.discover.domain.usecase.RefreshTeamsUseCase
import com.github.apkelly.drool.discover.domain.usecase.SetTeamFollowingUseCase
import com.github.apkelly.drool.home.data.HomeRepositoryImpl
import com.github.apkelly.drool.home.domain.usecase.ObserveFamilyClubsUseCase
import com.github.apkelly.drool.home.domain.usecase.ObserveFamilyTeamsUseCase
import com.github.apkelly.drool.match.data.MatchRepositoryImpl
import com.github.apkelly.drool.match.data.weather.MatchWeatherDataSource
import com.github.apkelly.drool.match.domain.usecase.LoadMatchDetailsUseCase
import com.github.apkelly.drool.match.domain.usecase.LoadMatchWeatherUseCase
import com.github.apkelly.drool.profile.data.ProfileRepositoryImpl
import com.github.apkelly.drool.profile.domain.repository.PreferencesRepository
import com.github.apkelly.drool.profile.domain.usecase.GetLinkCandidatesUseCase
import com.github.apkelly.drool.profile.domain.usecase.ObserveObservabilityEnabledUseCase
import com.github.apkelly.drool.profile.domain.usecase.ObserveThemeModeUseCase
import com.github.apkelly.drool.profile.domain.usecase.RequestLinkedUserUseCase
import com.github.apkelly.drool.profile.domain.usecase.SetObservabilityEnabledUseCase
import com.github.apkelly.drool.profile.domain.usecase.SetThemeModeUseCase
import com.github.apkelly.drool.profile.domain.usecase.UnlinkLinkedUserUseCase
import com.github.apkelly.drool.profile.domain.usecase.VerifyLinkedUserUseCase
import com.github.apkelly.drool.schedule.data.ScheduleRepositoryImpl
import com.github.apkelly.drool.schedule.domain.usecase.ObserveFixturesUseCase
import com.github.apkelly.drool.schedule.domain.usecase.RefreshFamilyProfilesUseCase
import com.github.apkelly.drool.schedule.domain.usecase.RefreshFixturesUseCase
import com.github.apkelly.drool.team.data.TeamRepositoryImpl
import com.github.apkelly.drool.team.domain.usecase.LoadTeamHubUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UseCasesTest {
    @Test
    fun sessionUseCasesDelegateToRepository() = runTest {
        val dataSource = FakeSessionDataSource()
        val authRepository = AuthRepositoryImpl(dataSource)
        val profileRepository = ProfileRepositoryImpl(dataSource)
        val credentials = AuthCredentials("base", "path", "user", "password")
        val session = AuthenticatedSession("token", Profile("1", "Alex", null))
        dataSource.signInResult = session
        dataSource.restoreResult = session

        assertEquals(dataSource.profile, ObserveProfileUseCase(authRepository)())
        assertEquals(session, RestoreSessionUseCase(authRepository)())
        assertEquals(session, SignInUserUseCase(authRepository)(credentials))
        assertEquals(credentials, dataSource.lastCredentials)
        SignOutUserUseCase(authRepository)()
        assertEquals(1, dataSource.signOutCount)
        dataSource.linkCandidates = listOf(
            RelatedUser("candidate", "Jude Bellingham", null, null),
        )
        assertEquals(dataSource.linkCandidates, GetLinkCandidatesUseCase(profileRepository)())
        assertEquals(
            LinkMemberRequestResult(
                relationshipId = "linked-candidate",
                isApproved = false,
            ),
            RequestLinkedUserUseCase(profileRepository)("candidate", true),
        )
        assertEquals("candidate" to true, dataSource.lastLinkRequest)
        VerifyLinkedUserUseCase(profileRepository)("candidate", "123456")
        assertEquals("candidate" to "123456", dataSource.lastLinkVerification)
        UnlinkLinkedUserUseCase(profileRepository)("linked-candidate")
        assertEquals("linked-candidate", dataSource.lastUnlinkedUser)
    }

    @Test
    fun sessionRestoreCanReturnNull() = runTest {
        assertNull(
            RestoreSessionUseCase(AuthRepositoryImpl(FakeSessionDataSource()))(),
        )
    }

    @Test
    fun preferenceUseCasesDelegateToRepository() = runTest {
        val repository = FakePreferencesRepository()
        val observe = ObserveThemeModeUseCase(repository)()
        assertEquals(ThemeMode.System, observe.first())

        SetThemeModeUseCase(repository)(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, repository.theme.value)

        assertEquals(false, ObserveObservabilityEnabledUseCase(repository)().first())
        SetObservabilityEnabledUseCase(repository)(true)
        assertEquals(true, repository.observabilityEnabled.value)
    }

    @Test
    fun sportsObservationUseCasesDelegateToRepository() {
        val dataSource = FakeSportsDataSource()
        val discover = DiscoverRepositoryImpl(dataSource)
        val schedule = ScheduleRepositoryImpl(dataSource)
        val home = HomeRepositoryImpl(dataSource)
        assertEquals(dataSource.clubs, ObserveClubsUseCase(discover)())
        assertEquals(dataSource.teams, ObserveTeamsUseCase(discover)("club"))
        assertEquals("club", dataSource.lastClubId)
        assertEquals(dataSource.teams, ObserveTeamsUseCase(discover)())
        assertEquals(null, dataSource.lastClubId)
        assertEquals(dataSource.fixtures, ObserveFixturesUseCase(schedule)())
        assertEquals(dataSource.familyFixtures, ObserveFixturesUseCase(schedule)(setOf("1")))
        assertEquals(setOf("1"), dataSource.lastProfileIds)
        assertEquals(dataSource.familyTeams, ObserveFamilyTeamsUseCase(home)(setOf("2")))
        assertEquals(setOf("2"), dataSource.lastProfileIds)
        assertEquals(dataSource.familyClubs, ObserveFamilyClubsUseCase(home)(setOf("3")))
        assertEquals(setOf("3"), dataSource.lastProfileIds)
        assertEquals(dataSource.relationships, ObserveTeamRelationshipsUseCase(discover)())
    }

    @Test
    fun sportsMutationUseCasesDelegateToRepository() = runTest {
        val dataSource = FakeSportsDataSource()
        val weatherDataSource = FakeMatchWeatherDataSource()
        val discover = DiscoverRepositoryImpl(dataSource)
        val schedule = ScheduleRepositoryImpl(dataSource)
        val team = TeamRepositoryImpl(dataSource)
        val match = MatchRepositoryImpl(dataSource, weatherDataSource)
        assertEquals(RefreshResult.Updated, RefreshClubsUseCase(discover)(false))
        assertEquals(false, dataSource.lastForce)
        assertEquals(RefreshResult.Updated, RefreshClubsUseCase(discover)())
        assertEquals(true, dataSource.lastForce)
        val profiles = listOf(FamilyProfile("1", "Alex", null, true))
        assertEquals(
            mapOf("1" to RefreshResult.Updated),
            RefreshFamilyProfilesUseCase(schedule)(profiles, false),
        )
        assertEquals(profiles, dataSource.lastFamilyProfiles)
        assertEquals(
            mapOf("1" to RefreshResult.Updated),
            RefreshFamilyProfilesUseCase(schedule)(profiles),
        )
        assertEquals(true, dataSource.lastForce)
        assertEquals(RefreshResult.Updated, RefreshTeamsUseCase(discover)(clubId = "club"))
        assertEquals(true, dataSource.lastForce)
        assertEquals("club", dataSource.lastRefreshClubId)
        assertEquals(RefreshResult.Updated, RefreshTeamsUseCase(discover)())
        assertEquals(null, dataSource.lastRefreshClubId)
        assertEquals(RefreshResult.Updated, RefreshFixturesUseCase(schedule)(false))
        assertEquals(false, dataSource.lastForce)
        assertEquals(RefreshResult.Updated, RefreshFixturesUseCase(schedule)())
        assertEquals(true, dataSource.lastForce)

        SetTeamFollowingUseCase(discover)("team", true)
        assertEquals("team" to true, dataSource.lastFollow)
        assertEquals(
            dataSource.teamHub,
            LoadTeamHubUseCase(team)("profile", "team").value,
        )
        assertEquals("profile" to "team", dataSource.lastTeamHub)
        assertEquals(
            dataSource.matchDetails,
            LoadMatchDetailsUseCase(match)("profile", "match"),
        )
        assertEquals("profile" to "match", dataSource.lastMatchDetails)
        assertEquals(
            weatherDataSource.weather,
            LoadMatchWeatherUseCase(match)(dataSource.matchDetails),
        )
        assertEquals(dataSource.matchDetails, weatherDataSource.lastFixture)
    }

    @Test
    fun sportsViewModelLoadsTeamHubCacheAndRefreshes() = runTest {
        val dataSource = FakeSportsDataSource()
        dataSource.teamHub = TeamHub(
            teamId = "team",
            matches = listOf(dataSource.matchDetails),
            results = emptyList(),
            ladderName = null,
            ladder = emptyList(),
        )
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val weatherDataSource = FakeMatchWeatherDataSource()
        val discover = DiscoverRepositoryImpl(dataSource)
        val schedule = ScheduleRepositoryImpl(dataSource)
        val home = HomeRepositoryImpl(dataSource)
        val team = TeamRepositoryImpl(dataSource)
        val match = MatchRepositoryImpl(dataSource, weatherDataSource)
        val viewModel = SportsViewModel(
            observeClubs = ObserveClubsUseCase(discover),
            observeTeams = ObserveTeamsUseCase(discover),
            observeFixtures = ObserveFixturesUseCase(schedule),
            observeFamilyTeams = ObserveFamilyTeamsUseCase(home),
            observeFamilyClubs = ObserveFamilyClubsUseCase(home),
            observeRelationships = ObserveTeamRelationshipsUseCase(discover),
            refreshClubsUseCase = RefreshClubsUseCase(discover),
            refreshTeamsUseCase = RefreshTeamsUseCase(discover),
            refreshFixturesUseCase = RefreshFixturesUseCase(schedule),
            refreshFamilyProfiles = RefreshFamilyProfilesUseCase(schedule),
            setTeamFollowing = SetTeamFollowingUseCase(discover),
            loadTeamHub = LoadTeamHubUseCase(team),
            loadMatchDetails = LoadMatchDetailsUseCase(match),
            loadMatchWeather = LoadMatchWeatherUseCase(match),
            scope = scope,
        )
        try {
            viewModel.loadTeam("profile", "team")
            advanceUntilIdle()
            viewModel.loadTeam("profile", "team")
            advanceUntilIdle()
            assertEquals(2, dataSource.teamHubCalls)
            assertEquals(false, dataSource.lastTeamHubForce)

            viewModel.refreshTeam("profile", "team")
            advanceUntilIdle()
            assertEquals(3, dataSource.teamHubCalls)
            assertEquals(true, dataSource.lastTeamHubForce)

            viewModel.loadMatch("profile", "match")
            advanceUntilIdle()
            assertEquals(1, dataSource.matchDetailsCalls)

            viewModel.loadMatch("profile", "uncached")
            advanceUntilIdle()
            viewModel.loadMatch("profile", "uncached")
            advanceUntilIdle()
            assertEquals(2, dataSource.matchDetailsCalls)
        } finally {
            scope.cancel()
        }
    }
}

private class FakeMatchWeatherDataSource : MatchWeatherDataSource {
    val weather = MatchWeather("Sunny", 16.0, 24.0, 55, 10, 0.0, 6)
    var lastFixture: Fixture? = null

    override suspend fun loadMatchWeather(
        fixture: Fixture,
    ): MatchWeather {
        lastFixture = fixture
        return weather
    }
}

private class FakeSessionDataSource : SessionDataSource {
    val profile = MutableStateFlow<Profile?>(null)
    var restoreResult: AuthenticatedSession? = null
    var signInResult = AuthenticatedSession("token", null)
    var lastCredentials: AuthCredentials? = null
    var signOutCount = 0
    var lastProfileRefresh: Pair<ProfileApiEndpoint, String>? = null
    var linkCandidates: List<RelatedUser> = emptyList()
    var lastLinkVerification: Pair<String, String>? = null
    var lastLinkRequest: Pair<String, Boolean>? = null
    var lastUnlinkedUser: String? = null

    override fun observeProfile(): Flow<Profile?> = profile
    override suspend fun restoreSession() = restoreResult
    override suspend fun signIn(
        credentials: AuthCredentials,
    ): AuthenticatedSession {
        lastCredentials = credentials
        return signInResult
    }

    override suspend fun signOut() {
        signOutCount += 1
    }

    override suspend fun refreshProfileEndpoint(
        endpoint: ProfileApiEndpoint,
        email: String,
    ) {
        lastProfileRefresh = endpoint to email
    }

    override suspend fun getLinkCandidates(): List<RelatedUser> = linkCandidates

    override suspend fun requestLinkedUser(
        candidateId: String,
        isGuardian: Boolean,
    ): LinkMemberRequestResult {
        lastLinkRequest = candidateId to isGuardian
        return LinkMemberRequestResult(
            relationshipId = "linked-$candidateId",
            isApproved = false,
        )
    }

    override suspend fun verifyLinkedUser(
        candidateId: String,
        token: String,
    ) {
        lastLinkVerification = candidateId to token
    }

    override suspend fun unlinkLinkedUser(
        linkId: String,
    ) {
        lastUnlinkedUser = linkId
    }
}

private class FakePreferencesRepository : PreferencesRepository {
    val theme = MutableStateFlow(ThemeMode.System)
    val observabilityEnabled = MutableStateFlow(false)
    override fun observeThemeMode(): Flow<ThemeMode> = theme
    override suspend fun setThemeMode(
        mode: ThemeMode,
    ) {
        theme.value = mode
    }
    override fun observeObservabilityEnabled(): Flow<Boolean> = observabilityEnabled
    override suspend fun setObservabilityEnabled(
        enabled: Boolean,
    ) {
        observabilityEnabled.value = enabled
    }
}

private class FakeSportsDataSource : SportsDataSource {
    val clubs = MutableStateFlow(CachedData<List<Club>>(emptyList(), null, true))
    val teams = MutableStateFlow(CachedData<List<Team>>(emptyList(), null, true))
    val fixtures = MutableStateFlow(CachedData<List<Fixture>>(emptyList(), null, true))
    val relationships = MutableStateFlow<Map<String, TeamRelationship>>(emptyMap())
    val familyFixtures = MutableStateFlow<List<Fixture>>(emptyList())
    val familyTeams = MutableStateFlow<List<FamilyTeam>>(emptyList())
    val familyClubs = MutableStateFlow<List<FamilyClub>>(emptyList())
    var lastClubId: String? = null
    var lastRefreshClubId: String? = null
    var lastForce: Boolean? = null
    var lastFollow: Pair<String, Boolean>? = null
    var lastProfileIds: Set<String>? = null
    var lastFamilyProfiles: List<FamilyProfile>? = null
    var lastTeamHub: Pair<String, String>? = null
    var lastTeamHubForce: Boolean? = null
    var lastMatchDetails: Pair<String, String>? = null
    var teamHub = TeamHub("team", emptyList(), emptyList(), null, emptyList())
    var teamHubCalls = 0
    var matchDetailsCalls = 0
    val matchDetails = Fixture(
        "match",
        null,
        0L,
        null,
        "Home",
        null,
        "Away",
        null,
        null,
        null,
        FixtureStatus.Scheduled,
    )

    override fun observeClubs(): Flow<CachedData<List<Club>>> = clubs
    override fun observeTeams(
        clubId: String?,
    ): Flow<CachedData<List<Team>>> {
        lastClubId = clubId
        return teams
    }

    override fun observeFixtures(): Flow<CachedData<List<Fixture>>> = fixtures
    override fun observeFamilyFixtures(
        profileIds: Set<String>,
    ): Flow<List<Fixture>> {
        lastProfileIds = profileIds
        return familyFixtures
    }

    override fun observeFamilyTeams(
        profileIds: Set<String>,
    ): Flow<List<FamilyTeam>> {
        lastProfileIds = profileIds
        return familyTeams
    }

    override fun observeFamilyClubs(
        profileIds: Set<String>,
    ): Flow<List<FamilyClub>> {
        lastProfileIds = profileIds
        return familyClubs
    }
    override fun observeTeamRelationships(): Flow<Map<String, TeamRelationship>> = relationships
    override suspend fun refreshClubs(
        force: Boolean,
    ): RefreshResult {
        lastForce = force
        return RefreshResult.Updated
    }

    override suspend fun refreshTeams(
        force: Boolean,
        clubId: String?,
    ): RefreshResult {
        lastForce = force
        lastRefreshClubId = clubId
        return RefreshResult.Updated
    }

    override suspend fun refreshFixtures(
        force: Boolean,
    ): RefreshResult {
        lastForce = force
        return RefreshResult.Updated
    }

    override suspend fun refreshFamilyProfiles(
        profiles: List<FamilyProfile>,
        force: Boolean,
    ): Map<String, RefreshResult> {
        lastFamilyProfiles = profiles
        lastForce = force
        return profiles.associate { it.id to RefreshResult.Updated }
    }

    override suspend fun setFollowing(
        teamId: String,
        following: Boolean,
    ) {
        lastFollow = teamId to following
    }

    override suspend fun loadTeamHub(
        profileId: String,
        teamId: String,
        force: Boolean,
    ): CachedData<TeamHub> {
        lastTeamHub = profileId to teamId
        lastTeamHubForce = force
        teamHubCalls += 1
        return CachedData(teamHub, lastUpdatedEpochMillis = 0L, isStale = false)
    }

    override suspend fun loadMatchDetails(
        profileId: String,
        matchId: String,
    ): Fixture {
        lastMatchDetails = profileId to matchId
        matchDetailsCalls += 1
        return matchDetails.copy(id = matchId)
    }
}
