package com.github.apkelly.drool.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.apkelly.drool.auth.domain.usecase.ObserveProfileUseCase
import com.github.apkelly.drool.auth.domain.usecase.RestoreSessionUseCase
import com.github.apkelly.drool.auth.domain.usecase.SignInUserUseCase
import com.github.apkelly.drool.auth.domain.usecase.SignOutUserUseCase
import com.github.apkelly.drool.auth.ui.model.AuthFailure
import com.github.apkelly.drool.auth.ui.model.SessionUiState
import com.github.apkelly.drool.common.domain.model.AssignmentPollingInterval
import com.github.apkelly.drool.common.domain.model.AuthCredentials
import com.github.apkelly.drool.common.domain.model.AuthenticationException
import com.github.apkelly.drool.common.domain.model.AuthenticationFailure
import com.github.apkelly.drool.common.domain.model.LinkMemberException
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.ThemeMode
import com.github.apkelly.drool.common.logging.DroolLog
import com.github.apkelly.drool.common.observability.AnalyticsEvent
import com.github.apkelly.drool.common.observability.AppScreen
import com.github.apkelly.drool.common.observability.NoOpObservability
import com.github.apkelly.drool.common.observability.NonFatalOperation
import com.github.apkelly.drool.common.observability.Observability
import com.github.apkelly.drool.common.observability.SignInOutcome
import com.github.apkelly.drool.common.observability.ThemeSelection
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
import com.github.apkelly.drool.profile.ui.model.LinkMemberUiState
import com.github.apkelly.drool.profile.ui.model.UnlinkMemberUiState
import com.github.apkelly.drool.schedule.background.AssignmentPollingScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val observeProfile: ObserveProfileUseCase,
    private val restoreSession: RestoreSessionUseCase,
    private val signInUser: SignInUserUseCase,
    private val signOutUser: SignOutUserUseCase,
    private val getLinkCandidates: GetLinkCandidatesUseCase,
    private val requestLinkedUserUseCase: RequestLinkedUserUseCase,
    private val verifyLinkedUserUseCase: VerifyLinkedUserUseCase,
    private val unlinkLinkedUserUseCase: UnlinkLinkedUserUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    observeObservabilityEnabled: ObserveObservabilityEnabledUseCase,
    private val setObservabilityEnabled: SetObservabilityEnabledUseCase,
    observeAssignmentPollingInterval: ObserveAssignmentPollingIntervalUseCase,
    private val setAssignmentPollingInterval: SetAssignmentPollingIntervalUseCase,
    assignmentPollingScheduler: AssignmentPollingScheduler,
    private val observability: Observability = NoOpObservability,
) : ViewModel() {
    private val logger = DroolLog.withTag("AppViewModel")
    private val mutableSessionState = MutableStateFlow<SessionUiState>(SessionUiState.Bootstrapping)
    val sessionState: StateFlow<SessionUiState> = mutableSessionState
    private val mutableLinkMemberState =
        MutableStateFlow<LinkMemberUiState>(LinkMemberUiState.Idle)
    val linkMemberState: StateFlow<LinkMemberUiState> = mutableLinkMemberState
    private val mutableUnlinkMemberState =
        MutableStateFlow<UnlinkMemberUiState>(UnlinkMemberUiState.Idle)
    val unlinkMemberState: StateFlow<UnlinkMemberUiState> = mutableUnlinkMemberState
    private var profileObservationJob: Job? = null

    val themeMode: StateFlow<ThemeMode> =
        observeThemeMode()
            .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)

    val observabilityEnabled: StateFlow<Boolean> =
        observeObservabilityEnabled()
            .distinctUntilChanged()
            .onEach(observability::setCollectionEnabled)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val assignmentPollingInterval: StateFlow<AssignmentPollingInterval> =
        observeAssignmentPollingInterval()
            .distinctUntilChanged()
            .onEach(assignmentPollingScheduler::schedule)
            .stateIn(viewModelScope, SharingStarted.Eagerly, AssignmentPollingInterval.Off)

    init {
        restore()
    }

    fun restore() {
        updateSessionState(SessionUiState.Bootstrapping)
        viewModelScope.launch {
            val state = try {
                val session = restoreSession()
                when {
                    session == null -> SessionUiState.AuthenticationRequired
                    session.profile == null -> SessionUiState.ReconnectRequired
                    else -> SessionUiState.Authenticated(session.profile)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (unexpected: Exception) {
                logger.w { "Session restoration failed" }
                observability.recordNonFatal(unexpected, NonFatalOperation.RestoreSession)
                SessionUiState.ReconnectRequired
            }
            updateSessionState(state)
        }
    }

    fun signIn(
        username: String,
        password: String,
    ) {
        if (username.isBlank() || password.isBlank()) {
            observability.log(
                AnalyticsEvent.SignInCompleted(SignInOutcome.MissingFields),
            )
            updateSessionState(
                SessionUiState.AuthenticationFailed(AuthFailure.MissingFields),
            )
            return
        }

        updateSessionState(SessionUiState.Authenticating)
        viewModelScope.launch {
            val state = try {
                val session = signInUser(
                    AuthCredentials(
                        baseUrl = "https://api.dribl.com/api",
                        path = "/auth/signin",
                        username = username.trim(),
                        password = password,
                    ),
                )
                if (session.profile == null) {
                    observability.log(
                        AnalyticsEvent.SignInCompleted(SignInOutcome.ReconnectRequired),
                    )
                    SessionUiState.ReconnectRequired
                } else {
                    observability.log(
                        AnalyticsEvent.SignInCompleted(SignInOutcome.Success),
                    )
                    SessionUiState.Authenticated(session.profile)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: AuthenticationException) {
                logger.w { "Authentication failed: ${error.reason}" }
                observability.log(
                    AnalyticsEvent.SignInCompleted(
                        when (error.reason) {
                            AuthenticationFailure.InvalidCredentials ->
                                SignInOutcome.InvalidCredentials

                            AuthenticationFailure.Offline -> SignInOutcome.Offline

                            AuthenticationFailure.Unknown -> SignInOutcome.UnknownFailure
                        },
                    ),
                )
                SessionUiState.AuthenticationFailed(
                    when (error.reason) {
                        AuthenticationFailure.InvalidCredentials -> AuthFailure.InvalidCredentials
                        AuthenticationFailure.Offline -> AuthFailure.Offline
                        AuthenticationFailure.Unknown -> AuthFailure.Unknown
                    },
                )
            } catch (unexpected: Exception) {
                logger.e { "Unexpected authentication failure" }
                observability.log(
                    AnalyticsEvent.SignInCompleted(SignInOutcome.UnknownFailure),
                )
                observability.recordNonFatal(unexpected, NonFatalOperation.SignIn)
                SessionUiState.AuthenticationFailed(AuthFailure.Unknown)
            }
            updateSessionState(state)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUser()
            observability.log(AnalyticsEvent.SignedOut)
            mutableLinkMemberState.value = LinkMemberUiState.Idle
            mutableUnlinkMemberState.value = UnlinkMemberUiState.Idle
            updateSessionState(SessionUiState.AuthenticationRequired)
        }
    }

    fun loadLinkCandidates() {
        mutableLinkMemberState.value = LinkMemberUiState.Loading
        viewModelScope.launch {
            mutableLinkMemberState.value = try {
                LinkMemberUiState.Ready(getLinkCandidates())
            } catch (error: CancellationException) {
                throw error
            } catch (error: LinkMemberException) {
                LinkMemberUiState.Failed(error.reason)
            }
        }
    }

    fun requestLinkedUser(
        candidate: RelatedUser,
        isGuardian: Boolean,
    ) {
        val current = mutableLinkMemberState.value as? LinkMemberUiState.Ready ?: return
        if (current.requestingCandidateId != null) return
        mutableLinkMemberState.value = current.copy(requestingCandidateId = candidate.id)
        viewModelScope.launch {
            mutableLinkMemberState.value = try {
                val result = requestLinkedUserUseCase(candidate.id, isGuardian)
                if (result.isApproved) {
                    LinkMemberUiState.Linked
                } else {
                    LinkMemberUiState.AwaitingVerification(
                        candidates = current.candidates,
                        candidate = candidate,
                        linkId = result.relationshipId,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: LinkMemberException) {
                LinkMemberUiState.Failed(error.reason)
            }
        }
    }

    fun verifyLinkedUser(
        token: String,
    ) {
        val current =
            mutableLinkMemberState.value as? LinkMemberUiState.AwaitingVerification ?: return
        if (token.length != LINK_TOKEN_LENGTH || !token.all(Char::isDigit)) return
        if (current.verifying) return
        mutableLinkMemberState.value = current.copy(verifying = true)
        viewModelScope.launch {
            mutableLinkMemberState.value = try {
                verifyLinkedUserUseCase(current.linkId, token)
                LinkMemberUiState.Linked
            } catch (error: CancellationException) {
                throw error
            } catch (error: LinkMemberException) {
                LinkMemberUiState.Failed(error.reason)
            }
        }
    }

    fun cancelLinkVerification() {
        val current =
            mutableLinkMemberState.value as? LinkMemberUiState.AwaitingVerification ?: return
        if (current.verifying) return
        mutableLinkMemberState.value = LinkMemberUiState.Ready(
            candidates = current.candidates,
        )
    }

    fun completeLinkMemberFlow() {
        if (mutableLinkMemberState.value != LinkMemberUiState.Linked) return
        mutableLinkMemberState.value = LinkMemberUiState.Idle
    }

    fun unlinkLinkedUser(
        linkId: String,
    ) {
        if (mutableUnlinkMemberState.value is UnlinkMemberUiState.Unlinking) return
        mutableUnlinkMemberState.value = UnlinkMemberUiState.Unlinking(linkId)
        viewModelScope.launch {
            mutableUnlinkMemberState.value = try {
                unlinkLinkedUserUseCase(linkId)
                UnlinkMemberUiState.Idle
            } catch (error: CancellationException) {
                throw error
            } catch (error: LinkMemberException) {
                UnlinkMemberUiState.Failed(error.reason)
            }
        }
    }

    fun clearUnlinkMemberFailure() {
        if (mutableUnlinkMemberState.value is UnlinkMemberUiState.Failed) {
            mutableUnlinkMemberState.value = UnlinkMemberUiState.Idle
        }
    }

    fun updateTheme(
        mode: ThemeMode,
    ) {
        viewModelScope.launch {
            setThemeMode(mode)
            observability.log(
                AnalyticsEvent.ThemeSelected(
                    when (mode) {
                        ThemeMode.System -> ThemeSelection.System
                        ThemeMode.Light -> ThemeSelection.Light
                        ThemeMode.Dark -> ThemeSelection.Dark
                    },
                ),
            )
        }
    }

    fun updateObservability(
        enabled: Boolean,
    ) {
        viewModelScope.launch { setObservabilityEnabled(enabled) }
    }

    fun updateAssignmentPollingInterval(
        interval: AssignmentPollingInterval,
    ) {
        viewModelScope.launch { setAssignmentPollingInterval(interval) }
    }

    fun trackScreen(
        screen: AppScreen,
    ) {
        observability.log(AnalyticsEvent.ScreenViewed(screen))
    }

    private fun updateSessionState(
        state: SessionUiState,
    ) {
        mutableSessionState.value = state
        profileObservationJob?.cancel()
        profileObservationJob = null
        val authenticated = state as? SessionUiState.Authenticated ?: return
        profileObservationJob = viewModelScope.launch {
            observeProfile().collect { profile ->
                if (profile?.accountId == authenticated.profile.accountId) {
                    mutableSessionState.value = SessionUiState.Authenticated(profile)
                }
            }
        }
    }

    private companion object {
        const val LINK_TOKEN_LENGTH = 6
    }
}
