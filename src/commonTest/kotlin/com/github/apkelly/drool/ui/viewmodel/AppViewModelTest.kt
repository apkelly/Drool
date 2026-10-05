package com.github.apkelly.drool.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.github.apkelly.drool.domain.model.AuthCredentials
import com.github.apkelly.drool.domain.model.AuthenticatedSession
import com.github.apkelly.drool.domain.model.AuthenticationException
import com.github.apkelly.drool.domain.model.AuthenticationFailure
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.domain.model.ThemeMode
import com.github.apkelly.drool.domain.model.RelatedUser
import com.github.apkelly.drool.domain.model.LinkMemberException
import com.github.apkelly.drool.domain.model.LinkMemberFailure
import com.github.apkelly.drool.domain.repository.PreferencesRepository
import com.github.apkelly.drool.domain.repository.SessionRepository
import com.github.apkelly.drool.domain.usecase.ObserveThemeModeUseCase
import com.github.apkelly.drool.domain.usecase.ObserveObservabilityEnabledUseCase
import com.github.apkelly.drool.domain.usecase.ObserveProfileUseCase
import com.github.apkelly.drool.domain.usecase.RestoreSessionUseCase
import com.github.apkelly.drool.domain.usecase.SetThemeModeUseCase
import com.github.apkelly.drool.domain.usecase.SetObservabilityEnabledUseCase
import com.github.apkelly.drool.domain.usecase.SignInUserUseCase
import com.github.apkelly.drool.domain.usecase.SignOutUserUseCase
import com.github.apkelly.drool.domain.usecase.RefreshProfileEndpointUseCase
import com.github.apkelly.drool.domain.usecase.GetLinkCandidatesUseCase
import com.github.apkelly.drool.domain.usecase.VerifyLinkedUserUseCase
import com.github.apkelly.drool.ui.model.AuthFailure
import com.github.apkelly.drool.ui.model.SessionUiState
import com.github.apkelly.drool.ui.model.LinkMemberUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @Test
    fun restoreSelectsTheCorrectOfflineSessionState() = runTest {
        val sessionRepository = FakeViewModelSessionRepository()
        val preferences = FakeViewModelPreferencesRepository()
        val viewModel = viewModel(sessionRepository, preferences)

        advanceUntilIdle()
        assertEquals(SessionUiState.AuthenticationRequired, viewModel.sessionState.value)

        sessionRepository.restoreResult = AuthenticatedSession("token", null)
        viewModel.restore()
        advanceUntilIdle()
        assertEquals(SessionUiState.ReconnectRequired, viewModel.sessionState.value)

        val profile = Profile("account", "Alex", null)
        sessionRepository.restoreResult = AuthenticatedSession("token", profile)
        viewModel.restore()
        advanceUntilIdle()
        assertEquals(SessionUiState.Authenticated(profile), viewModel.sessionState.value)

        val hydrated = profile.copy(
            relatedUsers = listOf(RelatedUser("child", "Ethan", null, null))
        )
        sessionRepository.observedProfile.value = hydrated
        advanceUntilIdle()
        assertEquals(SessionUiState.Authenticated(hydrated), viewModel.sessionState.value)
    }

    @Test
    fun signInValidatesAndClassifiesFailures() = runTest {
        val sessionRepository = FakeViewModelSessionRepository()
        val viewModel = viewModel(sessionRepository, FakeViewModelPreferencesRepository())
        advanceUntilIdle()

        viewModel.signIn("", "")
        assertEquals(
            SessionUiState.AuthenticationFailed(AuthFailure.MissingFields),
            viewModel.sessionState.value,
        )

        listOf(
            AuthenticationFailure.InvalidCredentials to AuthFailure.InvalidCredentials,
            AuthenticationFailure.Offline to AuthFailure.Offline,
            AuthenticationFailure.Unknown to AuthFailure.Unknown,
        ).forEach { (domainFailure, uiFailure) ->
            sessionRepository.signInFailure =
                AuthenticationException(domainFailure, IllegalStateException("failure"))
            viewModel.signIn(" alex ", "password")
            advanceUntilIdle()
            assertEquals(
                SessionUiState.AuthenticationFailed(uiFailure),
                viewModel.sessionState.value,
            )
        }

        sessionRepository.signInFailure = IllegalArgumentException("unexpected")
        viewModel.signIn("alex", "password")
        advanceUntilIdle()
        assertEquals(
            SessionUiState.AuthenticationFailed(AuthFailure.Unknown),
            viewModel.sessionState.value,
        )
    }

    @Test
    fun successfulSignInThemeAndSignOutUpdateState() = runTest {
        val profile = Profile("account", "Alex", null)
        val sessionRepository = FakeViewModelSessionRepository().apply {
            signInResult = AuthenticatedSession("token", profile)
        }

        val preferences = FakeViewModelPreferencesRepository()
        val viewModel = viewModel(sessionRepository, preferences)
        advanceUntilIdle()

        viewModel.signIn(" alex ", "password")
        advanceUntilIdle()
        assertEquals(SessionUiState.Authenticated(profile), viewModel.sessionState.value)
        assertEquals("alex", sessionRepository.credentials?.username)

        viewModel.updateTheme(ThemeMode.Dark)
        advanceUntilIdle()
        assertEquals(ThemeMode.Dark, preferences.theme.value)

        viewModel.updateObservability(true)
        advanceUntilIdle()
        assertEquals(true, preferences.observabilityEnabled.value)

        viewModel.refreshRelatedUsers(" alex@example.com ")
        viewModel.refreshAccessAccounts()
        viewModel.refreshLinkedUsers()
        advanceUntilIdle()
        assertEquals(
            listOf(
                ProfileApiEndpoint.RelatedUsers to "alex@example.com",
                ProfileApiEndpoint.AccessAccounts to "",
                ProfileApiEndpoint.LinkedUsers to "",
            ),
            sessionRepository.profileRefreshes,
        )

        viewModel.signOut()
        advanceUntilIdle()
        assertEquals(1, sessionRepository.signOutCount)
        assertEquals(SessionUiState.AuthenticationRequired, viewModel.sessionState.value)
        assertIs<SessionUiState.AuthenticationRequired>(viewModel.sessionState.value)
    }

    @Test
    fun linkMemberLoadsCandidatesValidatesTokenAndReportsFailures() = runTest {
        val sessions = FakeViewModelSessionRepository()
        val viewModel = viewModel(sessions, FakeViewModelPreferencesRepository())
        advanceUntilIdle()
        sessions.linkCandidates = listOf(RelatedUser("candidate", "Ethan", null, null))

        viewModel.loadLinkCandidates()
        advanceUntilIdle()
        assertEquals(
            LinkMemberUiState.Ready(sessions.linkCandidates),
            viewModel.linkMemberState.value,
        )

        viewModel.verifyLinkedUser("candidate", "123")
        assertEquals(null, sessions.verifiedLink)
        viewModel.verifyLinkedUser("candidate", "123456")
        advanceUntilIdle()
        assertEquals("candidate" to "123456", sessions.verifiedLink)
        assertEquals(LinkMemberUiState.Linked, viewModel.linkMemberState.value)

        sessions.linkFailure = LinkMemberException(LinkMemberFailure.SessionExpired)
        viewModel.loadLinkCandidates()
        advanceUntilIdle()
        assertEquals(
            LinkMemberUiState.Failed(LinkMemberFailure.SessionExpired),
            viewModel.linkMemberState.value,
        )
    }

    private fun kotlinx.coroutines.test.TestScope.viewModel(
        sessions: FakeViewModelSessionRepository,
        preferences: FakeViewModelPreferencesRepository,
    ) = AppViewModel(
        observeProfile = ObserveProfileUseCase(sessions),
        restoreSession = RestoreSessionUseCase(sessions),
        signInUser = SignInUserUseCase(sessions),
        signOutUser = SignOutUserUseCase(sessions),
        refreshProfileEndpoint = RefreshProfileEndpointUseCase(sessions),
        getLinkCandidates = GetLinkCandidatesUseCase(sessions),
        verifyLinkedUserUseCase = VerifyLinkedUserUseCase(sessions),
        observeThemeMode = ObserveThemeModeUseCase(preferences),
        setThemeMode = SetThemeModeUseCase(preferences),
        observeObservabilityEnabled = ObserveObservabilityEnabledUseCase(preferences),
        setObservabilityEnabled = SetObservabilityEnabledUseCase(preferences),
        scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)),
    )
}

private class FakeViewModelSessionRepository : SessionRepository {
    val observedProfile = MutableStateFlow<Profile?>(null)
    var restoreResult: AuthenticatedSession? = null
    var signInResult = AuthenticatedSession("token", null)
    var signInFailure: Throwable? = null
    var credentials: AuthCredentials? = null
    var signOutCount = 0
    val profileRefreshes = mutableListOf<Pair<ProfileApiEndpoint, String>>()
    var linkCandidates: List<RelatedUser> = emptyList()
    var verifiedLink: Pair<String, String>? = null
    var linkFailure: Throwable? = null

    override fun observeProfile(): Flow<Profile?> = observedProfile
    override suspend fun restoreSession(): AuthenticatedSession? = restoreResult

    override suspend fun signIn(credentials: AuthCredentials): AuthenticatedSession {
        signInFailure?.let { throw it }
        this.credentials = credentials
        return signInResult
    }

    override suspend fun signOut() {
        signOutCount += 1
    }

    override suspend fun refreshProfileEndpoint(endpoint: ProfileApiEndpoint, email: String) {
        profileRefreshes += endpoint to email
    }

    override suspend fun getLinkCandidates(): List<RelatedUser> {
        linkFailure?.let { throw it }
        return linkCandidates
    }

    override suspend fun verifyLinkedUser(candidateId: String, token: String) {
        linkFailure?.let { throw it }
        verifiedLink = candidateId to token
    }
}

private class FakeViewModelPreferencesRepository : PreferencesRepository {
    val theme = MutableStateFlow(ThemeMode.System)
    val observabilityEnabled = MutableStateFlow(false)

    override fun observeThemeMode(): Flow<ThemeMode> = theme

    override suspend fun setThemeMode(mode: ThemeMode) {
        theme.value = mode
    }

    override fun observeObservabilityEnabled(): Flow<Boolean> = observabilityEnabled

    override suspend fun setObservabilityEnabled(enabled: Boolean) {
        observabilityEnabled.value = enabled
    }
}
