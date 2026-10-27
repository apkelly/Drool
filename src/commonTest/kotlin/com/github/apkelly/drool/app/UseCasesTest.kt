package com.github.apkelly.drool.app

import com.github.apkelly.drool.auth.data.AuthRepositoryImpl
import com.github.apkelly.drool.auth.domain.usecase.ObserveProfileUseCase
import com.github.apkelly.drool.auth.domain.usecase.RestoreSessionUseCase
import com.github.apkelly.drool.auth.domain.usecase.SignInUserUseCase
import com.github.apkelly.drool.auth.domain.usecase.SignOutUserUseCase
import com.github.apkelly.drool.common.data.repository.SessionDataSource
import com.github.apkelly.drool.common.domain.model.AssignmentPollingInterval
import com.github.apkelly.drool.common.domain.model.AuthCredentials
import com.github.apkelly.drool.common.domain.model.AuthenticatedSession
import com.github.apkelly.drool.common.domain.model.CachedData
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import com.github.apkelly.drool.common.domain.model.LinkMemberRequestResult
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.common.domain.model.MatchSheet
import com.github.apkelly.drool.common.domain.model.MatchSheetGoalDraft
import com.github.apkelly.drool.common.domain.model.MatchSheetMember
import com.github.apkelly.drool.common.domain.model.MatchSheetOfficial
import com.github.apkelly.drool.common.domain.model.MatchSheetScore
import com.github.apkelly.drool.common.domain.model.MatchSheetTeam
import com.github.apkelly.drool.common.domain.model.MatchWeather
import com.github.apkelly.drool.common.domain.model.OperationResult
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.common.domain.model.ProfileClub
import com.github.apkelly.drool.common.domain.model.ProfileTeam
import com.github.apkelly.drool.common.domain.model.RefereeAllocationDecision
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.Team
import com.github.apkelly.drool.common.domain.model.TeamHub
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
import com.github.apkelly.drool.common.domain.model.TeamSheetStatus
import com.github.apkelly.drool.common.domain.model.ThemeMode
import com.github.apkelly.drool.discover.domain.repository.DiscoverRepository
import com.github.apkelly.drool.discover.domain.usecase.ObserveClubsUseCase
import com.github.apkelly.drool.discover.domain.usecase.ObserveTeamRelationshipsUseCase
import com.github.apkelly.drool.discover.domain.usecase.ObserveTeamsUseCase
import com.github.apkelly.drool.discover.domain.usecase.RefreshClubsUseCase
import com.github.apkelly.drool.discover.domain.usecase.RefreshTeamsUseCase
import com.github.apkelly.drool.discover.domain.usecase.SetTeamFollowingUseCase
import com.github.apkelly.drool.home.domain.repository.HomeRepository
import com.github.apkelly.drool.home.domain.usecase.ObserveProfileClubsUseCase
import com.github.apkelly.drool.home.domain.usecase.ObserveProfileTeamsUseCase
import com.github.apkelly.drool.match.domain.model.RefereeMatchPreset
import com.github.apkelly.drool.match.domain.model.RefereeMatchSession
import com.github.apkelly.drool.match.domain.model.RefereeTimerPhase
import com.github.apkelly.drool.match.domain.repository.MatchRepository
import com.github.apkelly.drool.match.domain.usecase.CheckInRefereeUseCase
import com.github.apkelly.drool.match.domain.usecase.LoadMatchDetailsUseCase
import com.github.apkelly.drool.match.domain.usecase.LoadMatchSheetUseCase
import com.github.apkelly.drool.match.domain.usecase.LoadMatchWeatherUseCase
import com.github.apkelly.drool.match.domain.usecase.RefereeMatchSessionUseCase
import com.github.apkelly.drool.match.domain.usecase.SubmitRefereeMatchReportUseCase
import com.github.apkelly.drool.match.domain.usecase.UpdateMatchSheetMemberUseCase
import com.github.apkelly.drool.match.domain.usecase.UpdateTeamSheetStatusUseCase
import com.github.apkelly.drool.match.ui.model.MatchSheetUiState
import com.github.apkelly.drool.match.ui.shouldShowRefereeChecklistTab
import com.github.apkelly.drool.match.ui.viewmodel.MatchCache
import com.github.apkelly.drool.match.ui.viewmodel.MatchViewModel
import com.github.apkelly.drool.profile.data.ProfileRepositoryImpl
import com.github.apkelly.drool.profile.domain.repository.PreferencesRepository
import com.github.apkelly.drool.profile.domain.usecase.GetLinkCandidatesUseCase
import com.github.apkelly.drool.profile.domain.usecase.ObserveAssignmentPollingIntervalUseCase
import com.github.apkelly.drool.profile.domain.usecase.ObserveObservabilityEnabledUseCase
import com.github.apkelly.drool.profile.domain.usecase.ObserveThemeModeUseCase
import com.github.apkelly.drool.profile.domain.usecase.RequestLinkedUserUseCase
import com.github.apkelly.drool.profile.domain.usecase.SetAssignmentPollingIntervalUseCase
import com.github.apkelly.drool.profile.domain.usecase.SetObservabilityEnabledUseCase
import com.github.apkelly.drool.profile.domain.usecase.SetThemeModeUseCase
import com.github.apkelly.drool.profile.domain.usecase.UnlinkLinkedUserUseCase
import com.github.apkelly.drool.profile.domain.usecase.VerifyLinkedUserUseCase
import com.github.apkelly.drool.schedule.domain.repository.ScheduleRepository
import com.github.apkelly.drool.schedule.domain.usecase.ObserveFixturesUseCase
import com.github.apkelly.drool.schedule.domain.usecase.RefreshFixturesUseCase
import com.github.apkelly.drool.schedule.domain.usecase.RefreshLinkedProfilesUseCase
import com.github.apkelly.drool.schedule.domain.usecase.UpdateRefereeAllocationUseCase
import com.github.apkelly.drool.team.domain.repository.TeamRepository
import com.github.apkelly.drool.team.domain.usecase.LoadTeamHubUseCase
import com.github.apkelly.drool.team.ui.viewmodel.TeamViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
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

        assertEquals(
            AssignmentPollingInterval.Hourly,
            ObserveAssignmentPollingIntervalUseCase(repository)().first(),
        )
        SetAssignmentPollingIntervalUseCase(repository)(
            AssignmentPollingInterval.EverySixHours,
        )
        assertEquals(
            AssignmentPollingInterval.EverySixHours,
            repository.assignmentPollingInterval.value,
        )
    }

    @Test
    fun sportsObservationUseCasesDelegateToRepository() {
        val dataSource = FakeSportsDataSource()
        val discover = dataSource
        val schedule = dataSource
        val home = dataSource
        assertEquals(dataSource.clubs, ObserveClubsUseCase(discover)())
        assertEquals(dataSource.teams, ObserveTeamsUseCase(discover)("club"))
        assertEquals("club", dataSource.lastClubId)
        assertEquals(dataSource.teams, ObserveTeamsUseCase(discover)())
        assertEquals(null, dataSource.lastClubId)
        assertEquals(dataSource.fixtures, ObserveFixturesUseCase(schedule)())
        assertEquals(dataSource.linkedProfileFixtures, ObserveFixturesUseCase(schedule)(setOf("1")))
        assertEquals(setOf("1"), dataSource.lastProfileIds)
        assertEquals(dataSource.profileTeams, ObserveProfileTeamsUseCase(home)(setOf("2")))
        assertEquals(setOf("2"), dataSource.lastProfileIds)
        assertEquals(dataSource.profileClubs, ObserveProfileClubsUseCase(home)(setOf("3")))
        assertEquals(setOf("3"), dataSource.lastProfileIds)
        assertEquals(dataSource.relationships, ObserveTeamRelationshipsUseCase(discover)())
    }

    @Test
    fun sportsMutationUseCasesDelegateToRepository() = runTest {
        val dataSource = FakeSportsDataSource()
        val discover = dataSource
        val schedule = dataSource
        val team = dataSource
        val match = dataSource
        assertEquals(RefreshResult.Updated, RefreshClubsUseCase(discover)(false))
        assertEquals(false, dataSource.lastForce)
        assertEquals(RefreshResult.Updated, RefreshClubsUseCase(discover)())
        assertEquals(true, dataSource.lastForce)
        val profiles = listOf(LinkedProfile("1", "Alex", null, true))
        assertEquals(
            mapOf("1" to RefreshResult.Updated),
            RefreshLinkedProfilesUseCase(schedule)(profiles, false),
        )
        assertEquals(profiles, dataSource.lastLinkedProfiles)
        assertEquals(
            mapOf("1" to RefreshResult.Updated),
            RefreshLinkedProfilesUseCase(schedule)(profiles),
        )
        assertEquals(true, dataSource.lastForce)
        assertEquals(
            RefreshResult.Updated,
            UpdateRefereeAllocationUseCase(schedule)(
                profiles.single(),
                "allocation-1",
                RefereeAllocationDecision.Accept,
            ),
        )
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
            (LoadTeamHubUseCase(team)("profile", "team") as OperationResult.Success).value.value,
        )
        assertEquals("profile" to "team", dataSource.lastTeamHub)
        assertEquals(
            dataSource.matchDetails,
            (LoadMatchDetailsUseCase(match)("profile", "match") as OperationResult.Success).value,
        )
        assertEquals("profile" to "match", dataSource.lastMatchDetails)
        assertEquals(
            dataSource.matchSheet,
            (
                LoadMatchSheetUseCase(match)("profile", dataSource.matchDetails) as
                    OperationResult.Success
                ).value,
        )
        assertEquals(
            dataSource.matchSheetMember.copy(available = false),
            (
                UpdateMatchSheetMemberUseCase(match)("profile", "member", false) as
                    OperationResult.Success
                ).value,
        )
        assertEquals(
            TeamSheetStatus.Confirmed,
            (
                UpdateTeamSheetStatusUseCase(match)(
                    "profile",
                    "sheet",
                    "home",
                    TeamSheetAction.Confirm,
                ) as OperationResult.Success
                ).value,
        )
        assertEquals(
            dataSource.weather,
            (
                LoadMatchWeatherUseCase(match)(dataSource.matchDetails) as
                    OperationResult.Success
                ).value,
        )
        assertEquals(dataSource.matchDetails, dataSource.lastWeatherFixture)
        assertEquals(false, dataSource.lastWeatherForce)
        LoadMatchWeatherUseCase(match)(dataSource.matchDetails, force = true)
        assertEquals(true, dataSource.lastWeatherForce)
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
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val matchCache = MatchCache()
        val teamViewModel = TeamViewModel(
            loadTeamHub = LoadTeamHubUseCase(dataSource),
            matchCache = matchCache,
        )
        val matchViewModel = MatchViewModel(
            loadMatchDetails = LoadMatchDetailsUseCase(dataSource),
            loadMatchWeather = LoadMatchWeatherUseCase(dataSource),
            loadMatchSheetUseCase = LoadMatchSheetUseCase(dataSource),
            updateMatchSheetMemberUseCase = UpdateMatchSheetMemberUseCase(dataSource),
            updateTeamSheetStatusUseCase = UpdateTeamSheetStatusUseCase(dataSource),
            matchCache = matchCache,
        )
        try {
            teamViewModel.load("profile", "team")
            advanceUntilIdle()
            teamViewModel.load("profile", "team")
            advanceUntilIdle()
            assertEquals(1, dataSource.teamHubCalls)
            assertEquals(false, dataSource.lastTeamHubForce)

            teamViewModel.refresh("profile", "team")
            advanceUntilIdle()
            assertEquals(2, dataSource.teamHubCalls)
            assertEquals(true, dataSource.lastTeamHubForce)

            matchViewModel.load("profile", "match")
            advanceUntilIdle()
            assertEquals(1, dataSource.matchDetailsCalls)

            matchViewModel.refresh("profile", "match")
            advanceUntilIdle()
            assertEquals(2, dataSource.matchDetailsCalls)
            assertEquals(true, dataSource.lastWeatherForce)

            matchViewModel.load("profile", "uncached")
            advanceUntilIdle()
            matchViewModel.load("profile", "uncached")
            advanceUntilIdle()
            assertEquals(3, dataSource.matchDetailsCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun refereeChecklistVisibilityAndWorkflowUpdatesTimerGoalsAndSubmitsReport() = runTest {
        val kickoff = 1_700_000_000_000L
        val refereeFixture = Fixture(
            id = "match",
            profileId = "profile",
            kickoffEpochMillis = kickoff,
            homeTeamId = "home",
            homeTeamName = "Home",
            awayTeamId = "away",
            awayTeamName = "Away",
            competitionName = "Premier",
            venueName = "Main Field",
            userTeamId = null,
            status = FixtureStatus.Scheduled,
            role = FixtureRole.Referee,
            matchSheetId = "sheet",
        )
        assertEquals(
            false,
            shouldShowRefereeChecklistTab(refereeFixture, kickoff - 31 * 60_000L),
        )
        assertEquals(
            true,
            shouldShowRefereeChecklistTab(refereeFixture, kickoff - 30 * 60_000L),
        )
        assertEquals(
            true,
            shouldShowRefereeChecklistTab(refereeFixture, kickoff + 60 * 60_000L),
        )
        assertEquals(
            false,
            shouldShowRefereeChecklistTab(
                refereeFixture.copy(role = FixtureRole.Player),
                kickoff,
            ),
        )

        val dataSource = FakeSportsDataSource()
        dataSource.matchSheet = dataSource.matchSheet.copy(
            members = listOf(dataSource.matchSheetMember),
            referees = listOf(
                MatchSheetOfficial(
                    id = "ref-1",
                    displayName = "Ethan Referee",
                    roleName = "Centre Referee",
                    userId = "profile",
                    checkedIn = false,
                ),
            ),
        )
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val matchCache = MatchCache()
        val matchViewModel = MatchViewModel(
            loadMatchDetails = LoadMatchDetailsUseCase(dataSource),
            loadMatchWeather = LoadMatchWeatherUseCase(dataSource),
            loadMatchSheetUseCase = LoadMatchSheetUseCase(dataSource),
            updateMatchSheetMemberUseCase = UpdateMatchSheetMemberUseCase(dataSource),
            updateTeamSheetStatusUseCase = UpdateTeamSheetStatusUseCase(dataSource),
            matchCache = matchCache,
            checkInRefereeUseCase = CheckInRefereeUseCase(dataSource),
            submitRefereeMatchReportUseCase = SubmitRefereeMatchReportUseCase(dataSource),
            refereeMatchSessionUseCase = RefereeMatchSessionUseCase(dataSource),
        )
        try {
            matchViewModel.load("profile", "match")
            advanceUntilIdle()
            matchViewModel.loadMatchSheet("profile", force = true)
            advanceUntilIdle()

            matchViewModel.checkInReferee("profile", "ref-1")
            advanceUntilIdle()
            assertEquals("ref-1" to true, dataSource.lastCheckedInReferee)
            val checkedInState = matchViewModel.matchSheetState.value as MatchSheetUiState.Content
            assertEquals(true, checkedInState.matchSheet.referees.single().checkedIn)

            matchViewModel.selectRefereePreset("profile", RefereeMatchPreset.Adult)
            matchViewModel.toggleRefereeTimer("profile", 1_000L)
            matchViewModel.recordRefereeGoal(
                profileId = "profile",
                isHomeScoringTeam = true,
                member = dataSource.matchSheetMember,
                ownGoal = false,
                nowEpochMillis = 61_000L,
            )
            matchViewModel.advanceRefereePhase("profile", 90_000L)
            matchViewModel.advanceRefereePhase("profile", 100_000L)
            matchViewModel.recordRefereeGoal(
                profileId = "profile",
                isHomeScoringTeam = false,
                member = dataSource.matchSheetMember,
                ownGoal = true,
                nowEpochMillis = 160_000L,
            )
            matchViewModel.advanceRefereePhase("profile", 200_000L)
            advanceUntilIdle()

            val preSubmit = matchViewModel.matchSheetState.value as MatchSheetUiState.Content
            assertEquals(RefereeTimerPhase.FullTime, preSubmit.refereeSession.phase)
            assertEquals(1, preSubmit.refereeSession.homeHalfTimeScore)
            assertEquals(0, preSubmit.refereeSession.awayHalfTimeScore)
            assertEquals(1, preSubmit.refereeSession.homeFullTimeScore)
            assertEquals(1, preSubmit.refereeSession.awayFullTimeScore)

            matchViewModel.submitRefereeMatchReport("profile", 210_000L)
            advanceUntilIdle()

            assertEquals(1 to 1, dataSource.lastSubmittedScore)
            assertEquals(2, dataSource.lastSubmittedGoals?.size)
            val postSubmit = matchViewModel.matchSheetState.value as MatchSheetUiState.Content
            assertEquals(true, postSubmit.refereeReportSubmitted)

            matchViewModel.resetRefereeTimer("profile")
            matchViewModel.selectRefereePreset("profile", RefereeMatchPreset.Under18s)
            matchViewModel.toggleRefereeTimer("profile", 10_000L)
            matchViewModel.configureAccount("account-1")
            val runningSession =
                (matchViewModel.matchSheetState.value as MatchSheetUiState.Content).refereeSession
            assertEquals(true, runningSession.isRunning)
            assertEquals(300_000L, runningSession.elapsedHalfMillis(310_000L))

            matchViewModel.recordRefereeGoal(
                profileId = "profile",
                isHomeScoringTeam = true,
                member = null,
                ownGoal = true,
                nowEpochMillis = 70_000L,
            )
            val kidsGoalState = matchViewModel.matchSheetState.value as MatchSheetUiState.Content
            val kidsGoal = kidsGoalState.refereeSession.recordedGoals.last()
            assertEquals(true, kidsGoal.ownGoal)
            matchViewModel.toggleRefereeGoalOwnGoal("profile", kidsGoal.id)
            val toggledState = matchViewModel.matchSheetState.value as MatchSheetUiState.Content
            assertEquals(false, toggledState.refereeSession.recordedGoals.last().ownGoal)
            matchViewModel.toggleRefereeGoalOwnGoal("profile", kidsGoal.id)
            val retoggledState = matchViewModel.matchSheetState.value as MatchSheetUiState.Content
            assertEquals(true, retoggledState.refereeSession.recordedGoals.last().ownGoal)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

internal class FakeSessionDataSource : SessionDataSource {
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
    val assignmentPollingInterval = MutableStateFlow(AssignmentPollingInterval.Hourly)
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
    override fun observeAssignmentPollingInterval(): Flow<AssignmentPollingInterval> = assignmentPollingInterval
    override suspend fun setAssignmentPollingInterval(
        interval: AssignmentPollingInterval,
    ) {
        assignmentPollingInterval.value = interval
    }
}

internal class FakeSportsDataSource :
    DiscoverRepository,
    HomeRepository,
    ScheduleRepository,
    TeamRepository,
    MatchRepository {
    val clubs = MutableStateFlow(CachedData<List<Club>>(emptyList(), null, true))
    val teams = MutableStateFlow(CachedData<List<Team>>(emptyList(), null, true))
    val fixtures = MutableStateFlow(CachedData<List<Fixture>>(emptyList(), null, true))
    val relationships = MutableStateFlow<Map<String, TeamRelationship>>(emptyMap())
    val linkedProfileFixtures = MutableStateFlow(CachedData<List<Fixture>>(emptyList(), null, true))
    val profileTeams = MutableStateFlow<List<ProfileTeam>>(emptyList())
    val profileClubs = MutableStateFlow<List<ProfileClub>>(emptyList())
    var lastClubId: String? = null
    var lastRefreshClubId: String? = null
    var lastForce: Boolean? = null
    var lastFollow: Pair<String, Boolean>? = null
    var lastProfileIds: Set<String>? = null
    var lastLinkedProfiles: List<LinkedProfile>? = null
    var lastTeamHub: Pair<String, String>? = null
    var lastTeamHubForce: Boolean? = null
    var lastMatchDetails: Pair<String, String>? = null
    var linkedProfileRefreshResults: Map<String, RefreshResult>? = null
    var teamHub = TeamHub("team", emptyList(), emptyList(), null, emptyList())
    var teamHubCalls = 0
    var matchDetailsCalls = 0
    val weather = MatchWeather("Sunny", 16.0, 24.0, 55, 10, 0.0, 6)
    var lastWeatherFixture: Fixture? = null
    var lastWeatherForce = false
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
    var matchSheet = MatchSheet(
        id = "sheet",
        status = "pending",
        homeTeam = MatchSheetTeam("home", "Home", TeamSheetStatus.Open),
        awayTeam = MatchSheetTeam("away", "Away", TeamSheetStatus.Open),
        members = emptyList(),
        referees = emptyList(),
        officials = emptyList(),
    )
    val matchSheetMember = MatchSheetMember(
        id = "member",
        userId = "user",
        teamId = "home",
        firstName = "Alex",
        lastName = "Player",
        imageUrl = null,
        roleName = "Player",
        jerseyNumber = "7",
        available = true,
    )

    override fun observeClubs(): Flow<CachedData<List<Club>>> = clubs
    override fun observeTeams(
        clubId: String?,
    ): Flow<CachedData<List<Team>>> {
        lastClubId = clubId
        return teams
    }

    override fun observeFixtures(): Flow<CachedData<List<Fixture>>> = fixtures
    override fun observeLinkedProfileFixtures(
        profileIds: Set<String>,
    ): Flow<CachedData<List<Fixture>>> {
        lastProfileIds = profileIds
        return linkedProfileFixtures
    }

    override fun observeProfileTeams(
        profileIds: Set<String>,
    ): Flow<List<ProfileTeam>> {
        lastProfileIds = profileIds
        return profileTeams
    }

    override fun observeProfileClubs(
        profileIds: Set<String>,
    ): Flow<List<ProfileClub>> {
        lastProfileIds = profileIds
        return profileClubs
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

    override suspend fun refreshLinkedProfiles(
        profiles: List<LinkedProfile>,
        force: Boolean,
    ): Map<String, RefreshResult> {
        lastLinkedProfiles = profiles
        lastForce = force
        return linkedProfileRefreshResults
            ?: profiles.associate { it.id to RefreshResult.Updated }
    }

    override suspend fun updateRefereeAllocation(
        profile: LinkedProfile,
        allocationId: String,
        decision: RefereeAllocationDecision,
    ) = RefreshResult.Updated

    override suspend fun setFollowing(
        teamId: String,
        following: Boolean,
    ) {
        lastFollow = teamId to following
    }

    override suspend fun loadTeamHub(
        profileId: String,
        teamId: String,
        competitionName: String?,
        leagueId: String?,
        force: Boolean,
    ): OperationResult<CachedData<TeamHub>> {
        lastTeamHub = profileId to teamId
        lastTeamHubForce = force
        teamHubCalls += 1
        return OperationResult.Success(
            CachedData(teamHub, lastUpdatedEpochMillis = 0L, isStale = false),
        )
    }

    override suspend fun loadMatchDetails(
        profileId: String,
        matchId: String,
    ): OperationResult<Fixture> {
        lastMatchDetails = profileId to matchId
        matchDetailsCalls += 1
        return OperationResult.Success(matchDetails.copy(id = matchId))
    }

    override suspend fun loadMatchWeather(
        fixture: Fixture,
        force: Boolean,
    ): OperationResult<MatchWeather?> {
        lastWeatherFixture = fixture
        lastWeatherForce = force
        return OperationResult.Success(weather)
    }

    override suspend fun loadMatchSheet(
        profileId: String,
        fixture: Fixture,
    ): OperationResult<MatchSheet> = OperationResult.Success(matchSheet)

    override suspend fun updateMemberAvailability(
        profileId: String,
        memberId: String,
        available: Boolean,
    ): OperationResult<MatchSheetMember> = OperationResult.Success(
        matchSheetMember.copy(available = available),
    )

    override suspend fun updateTeamSheetStatus(
        profileId: String,
        matchSheetId: String,
        teamId: String,
        action: TeamSheetAction,
    ): OperationResult<TeamSheetStatus> = OperationResult.Success(
        when (action) {
            TeamSheetAction.Submit -> TeamSheetStatus.Submitted

            TeamSheetAction.Confirm -> TeamSheetStatus.Confirmed

            TeamSheetAction.Unsubmit,
            TeamSheetAction.Unconfirm,
            -> TeamSheetStatus.Open
        },
    )

    var lastCheckedInReferee: Pair<String, Boolean>? = null
    var lastSubmittedScore: Pair<Int, Int>? = null
    var lastSubmittedGoals: List<MatchSheetGoalDraft>? = null
    val savedSessions = mutableMapOf<Pair<String, String>, RefereeMatchSession>()

    override suspend fun updateRefereeCheckIn(
        profileId: String,
        refereeOfficialId: String,
        checkedIn: Boolean,
    ): OperationResult<Unit> {
        lastCheckedInReferee = refereeOfficialId to checkedIn
        return OperationResult.Success(Unit)
    }

    override suspend fun submitRefereeMatchReport(
        profileId: String,
        fixture: Fixture,
        scoreId: String?,
        homeHalfTime: Int?,
        awayHalfTime: Int?,
        homeFullTime: Int,
        awayFullTime: Int,
        goals: List<MatchSheetGoalDraft>?,
        existingGoalIds: List<String>,
    ): OperationResult<MatchSheet> {
        lastSubmittedScore = homeFullTime to awayFullTime
        lastSubmittedGoals = goals
        return OperationResult.Success(
            matchSheet.copy(
                score = MatchSheetScore(
                    id = scoreId ?: "score-1",
                    homeHalfTime = homeHalfTime,
                    awayHalfTime = awayHalfTime,
                    homeFullTime = homeFullTime,
                    awayFullTime = awayFullTime,
                ),
            ),
        )
    }

    override suspend fun loadRefereeMatchSession(
        profileId: String,
        matchId: String,
    ): RefereeMatchSession? = savedSessions[profileId to matchId]

    override suspend fun saveRefereeMatchSession(
        session: RefereeMatchSession,
    ) {
        savedSessions[session.profileId to session.matchId] = session
    }

    override suspend fun clearRefereeMatchSession(
        profileId: String,
        matchId: String,
    ) {
        savedSessions.remove(profileId to matchId)
    }
}
