package com.github.apkelly.drool.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.github.apkelly.drool.domain.model.AuthCredentials
import com.github.apkelly.drool.domain.model.AuthenticatedSession
import com.github.apkelly.drool.domain.model.CachedData
import com.github.apkelly.drool.domain.model.Club
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FamilyProfile
import com.github.apkelly.drool.domain.model.FamilyTeam
import com.github.apkelly.drool.domain.model.FamilyClub
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.domain.model.RelatedUser
import com.github.apkelly.drool.domain.model.RefreshResult
import com.github.apkelly.drool.domain.model.Team
import com.github.apkelly.drool.domain.model.TeamRelationship
import com.github.apkelly.drool.domain.model.TeamHub
import com.github.apkelly.drool.domain.model.FixtureStatus
import com.github.apkelly.drool.domain.model.ThemeMode
import com.github.apkelly.drool.domain.repository.PreferencesRepository
import com.github.apkelly.drool.domain.repository.SessionRepository
import com.github.apkelly.drool.domain.repository.SportsRepository
import com.github.apkelly.drool.ui.viewmodel.SportsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UseCasesTest {
    @Test
    fun sessionUseCasesDelegateToRepository() = runTest {
        val repository = FakeSessionRepository()
        val credentials = AuthCredentials("base", "path", "user", "password")
        val session = AuthenticatedSession("token", Profile("1", "Alex", null))
        repository.signInResult = session
        repository.restoreResult = session

        assertEquals(repository.profile, ObserveProfileUseCase(repository)())
        assertEquals(session, RestoreSessionUseCase(repository)())
        assertEquals(session, SignInUserUseCase(repository)(credentials))
        assertEquals(credentials, repository.lastCredentials)
        SignOutUserUseCase(repository)()
        assertEquals(1, repository.signOutCount)
        RefreshProfileEndpointUseCase(repository)(
            ProfileApiEndpoint.RelatedUsers,
            "alex@example.com",
        )
        assertEquals(
            ProfileApiEndpoint.RelatedUsers to "alex@example.com",
            repository.lastProfileRefresh,
        )
        repository.linkCandidates = listOf(
            RelatedUser("candidate", "Ethan", null, null)
        )
        assertEquals(repository.linkCandidates, GetLinkCandidatesUseCase(repository)())
        VerifyLinkedUserUseCase(repository)("candidate", "123456")
        assertEquals("candidate" to "123456", repository.lastLinkVerification)
    }

    @Test
    fun sessionRestoreCanReturnNull() = runTest {
        assertNull(RestoreSessionUseCase(FakeSessionRepository())())
    }

    @Test
    fun preferenceUseCasesDelegateToRepository() = runTest {
        val repository = FakePreferencesRepository()
        val observe = ObserveThemeModeUseCase(repository)()
        assertEquals(ThemeMode.System, observe.first())

        SetThemeModeUseCase(repository)(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, repository.theme.value)
    }

    @Test
    fun sportsObservationUseCasesDelegateToRepository() {
        val repository = FakeSportsRepository()
        assertEquals(repository.clubs, ObserveClubsUseCase(repository)())
        assertEquals(repository.teams, ObserveTeamsUseCase(repository)("club"))
        assertEquals("club", repository.lastClubId)
        assertEquals(repository.fixtures, ObserveFixturesUseCase(repository)())
        assertEquals(repository.familyFixtures, ObserveFixturesUseCase(repository)(setOf("1")))
        assertEquals(setOf("1"), repository.lastProfileIds)
        assertEquals(repository.familyTeams, ObserveFamilyTeamsUseCase(repository)(setOf("2")))
        assertEquals(setOf("2"), repository.lastProfileIds)
        assertEquals(repository.familyClubs, ObserveFamilyClubsUseCase(repository)(setOf("3")))
        assertEquals(setOf("3"), repository.lastProfileIds)
        assertEquals(repository.relationships, ObserveTeamRelationshipsUseCase(repository)())
    }

    @Test
    fun sportsMutationUseCasesDelegateToRepository() = runTest {
        val repository = FakeSportsRepository()
        assertEquals(RefreshResult.Updated, RefreshClubsUseCase(repository)(false))
        assertEquals(false, repository.lastForce)
        val profiles = listOf(FamilyProfile("1", "Alex", null, true))
        assertEquals(
            mapOf("1" to RefreshResult.Updated),
            RefreshFamilyProfilesUseCase(repository)(profiles, false),
        )
        assertEquals(profiles, repository.lastFamilyProfiles)
        assertEquals(
            mapOf("1" to RefreshResult.Updated),
            RefreshFamilyProfilesUseCase(repository)(profiles),
        )
        assertEquals(true, repository.lastForce)
        assertEquals(RefreshResult.Updated, RefreshTeamsUseCase(repository)(clubId = "club"))
        assertEquals(true, repository.lastForce)
        assertEquals("club", repository.lastRefreshClubId)
        assertEquals(RefreshResult.Updated, RefreshFixturesUseCase(repository)(false))
        assertEquals(false, repository.lastForce)

        SetTeamFollowingUseCase(repository)("team", true)
        assertEquals("team" to true, repository.lastFollow)
        assertEquals(repository.teamHub, LoadTeamHubUseCase(repository)("profile", "team"))
        assertEquals("profile" to "team", repository.lastTeamHub)
        assertEquals(
            repository.matchDetails,
            LoadMatchDetailsUseCase(repository)("profile", "match"),
        )
        assertEquals("profile" to "match", repository.lastMatchDetails)
    }

    @Test
    fun sportsViewModelCachesTeamHubsAndMatchDetails() = runTest {
        val repository = FakeSportsRepository()
        repository.teamHub = TeamHub(
            teamId = "team",
            matches = listOf(repository.matchDetails),
            results = emptyList(),
            ladderName = null,
            ladder = emptyList(),
        )
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = SportsViewModel(
            observeClubs = ObserveClubsUseCase(repository),
            observeTeams = ObserveTeamsUseCase(repository),
            observeFixtures = ObserveFixturesUseCase(repository),
            observeFamilyTeams = ObserveFamilyTeamsUseCase(repository),
            observeFamilyClubs = ObserveFamilyClubsUseCase(repository),
            observeRelationships = ObserveTeamRelationshipsUseCase(repository),
            refreshClubsUseCase = RefreshClubsUseCase(repository),
            refreshTeamsUseCase = RefreshTeamsUseCase(repository),
            refreshFixturesUseCase = RefreshFixturesUseCase(repository),
            refreshFamilyProfiles = RefreshFamilyProfilesUseCase(repository),
            setTeamFollowing = SetTeamFollowingUseCase(repository),
            loadTeamHub = LoadTeamHubUseCase(repository),
            loadMatchDetails = LoadMatchDetailsUseCase(repository),
            scope = scope,
        )
        try {
            viewModel.loadTeam("profile", "team")
            advanceUntilIdle()
            viewModel.loadTeam("profile", "team")
            advanceUntilIdle()
            assertEquals(1, repository.teamHubCalls)

            viewModel.loadMatch("profile", "match")
            advanceUntilIdle()
            assertEquals(0, repository.matchDetailsCalls)

            viewModel.loadMatch("profile", "uncached")
            advanceUntilIdle()
            viewModel.loadMatch("profile", "uncached")
            advanceUntilIdle()
            assertEquals(1, repository.matchDetailsCalls)
        } finally {
            scope.cancel()
        }
    }
}

private class FakeSessionRepository : SessionRepository {
    val profile = MutableStateFlow<Profile?>(null)
    var restoreResult: AuthenticatedSession? = null
    var signInResult = AuthenticatedSession("token", null)
    var lastCredentials: AuthCredentials? = null
    var signOutCount = 0
    var lastProfileRefresh: Pair<ProfileApiEndpoint, String>? = null
    var linkCandidates: List<RelatedUser> = emptyList()
    var lastLinkVerification: Pair<String, String>? = null

    override fun observeProfile(): Flow<Profile?> = profile
    override suspend fun restoreSession() = restoreResult
    override suspend fun signIn(credentials: AuthCredentials): AuthenticatedSession {
        lastCredentials = credentials
        return signInResult
    }

    override suspend fun signOut() {
        signOutCount += 1
    }

    override suspend fun refreshProfileEndpoint(endpoint: ProfileApiEndpoint, email: String) {
        lastProfileRefresh = endpoint to email
    }

    override suspend fun getLinkCandidates(): List<RelatedUser> = linkCandidates

    override suspend fun verifyLinkedUser(candidateId: String, token: String) {
        lastLinkVerification = candidateId to token
    }
}

private class FakePreferencesRepository : PreferencesRepository {
    val theme = MutableStateFlow(ThemeMode.System)
    override fun observeThemeMode(): Flow<ThemeMode> = theme
    override suspend fun setThemeMode(mode: ThemeMode) {
        theme.value = mode
    }
}

private class FakeSportsRepository : SportsRepository {
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
    override fun observeTeams(clubId: String?): Flow<CachedData<List<Team>>> {
        lastClubId = clubId
        return teams
    }

    override fun observeFixtures(): Flow<CachedData<List<Fixture>>> = fixtures
    override fun observeFamilyFixtures(profileIds: Set<String>): Flow<List<Fixture>> {
        lastProfileIds = profileIds
        return familyFixtures
    }

    override fun observeFamilyTeams(profileIds: Set<String>): Flow<List<FamilyTeam>> {
        lastProfileIds = profileIds
        return familyTeams
    }

    override fun observeFamilyClubs(profileIds: Set<String>): Flow<List<FamilyClub>> {
        lastProfileIds = profileIds
        return familyClubs
    }
    override fun observeTeamRelationships(): Flow<Map<String, TeamRelationship>> = relationships
    override suspend fun refreshClubs(force: Boolean): RefreshResult {
        lastForce = force
        return RefreshResult.Updated
    }

    override suspend fun refreshTeams(force: Boolean, clubId: String?): RefreshResult {
        lastForce = force
        lastRefreshClubId = clubId
        return RefreshResult.Updated
    }

    override suspend fun refreshFixtures(force: Boolean): RefreshResult {
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

    override suspend fun setFollowing(teamId: String, following: Boolean) {
        lastFollow = teamId to following
    }

    override suspend fun loadTeamHub(profileId: String, teamId: String): TeamHub {
        lastTeamHub = profileId to teamId
        teamHubCalls += 1
        return teamHub
    }

    override suspend fun loadMatchDetails(profileId: String, matchId: String): Fixture {
        lastMatchDetails = profileId to matchId
        matchDetailsCalls += 1
        return matchDetails.copy(id = matchId)
    }
}
