package com.github.apkelly.drool.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.github.apkelly.drool.domain.model.AuthCredentials
import com.github.apkelly.drool.domain.model.AuthenticationException
import com.github.apkelly.drool.domain.model.AuthenticationFailure
import com.github.apkelly.drool.domain.model.ThemeMode
import com.github.apkelly.drool.domain.usecase.ObserveThemeModeUseCase
import com.github.apkelly.drool.domain.usecase.ObserveProfileUseCase
import com.github.apkelly.drool.domain.usecase.RestoreSessionUseCase
import com.github.apkelly.drool.domain.usecase.SetThemeModeUseCase
import com.github.apkelly.drool.domain.usecase.SignInUserUseCase
import com.github.apkelly.drool.domain.usecase.SignOutUserUseCase
import com.github.apkelly.drool.domain.usecase.RefreshProfileEndpointUseCase
import com.github.apkelly.drool.domain.usecase.GetLinkCandidatesUseCase
import com.github.apkelly.drool.domain.usecase.VerifyLinkedUserUseCase
import com.github.apkelly.drool.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.domain.model.LinkMemberException
import com.github.apkelly.drool.logging.DroolLog
import com.github.apkelly.drool.ui.model.AuthFailure
import com.github.apkelly.drool.ui.model.SessionUiState
import com.github.apkelly.drool.ui.model.LinkMemberUiState

class AppViewModel(
    private val observeProfile: ObserveProfileUseCase,
    private val restoreSession: RestoreSessionUseCase,
    private val signInUser: SignInUserUseCase,
    private val signOutUser: SignOutUserUseCase,
    private val refreshProfileEndpoint: RefreshProfileEndpointUseCase,
    private val getLinkCandidates: GetLinkCandidatesUseCase,
    private val verifyLinkedUserUseCase: VerifyLinkedUserUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val logger = DroolLog.withTag("AppViewModel")
    private val mutableSessionState = MutableStateFlow<SessionUiState>(SessionUiState.Bootstrapping)
    val sessionState: StateFlow<SessionUiState> = mutableSessionState
    private val mutableLinkMemberState =
        MutableStateFlow<LinkMemberUiState>(LinkMemberUiState.Idle)
    val linkMemberState: StateFlow<LinkMemberUiState> = mutableLinkMemberState
    private var profileObservationJob: Job? = null

    val themeMode: StateFlow<ThemeMode> =
        observeThemeMode()
            .stateIn(scope, SharingStarted.Eagerly, ThemeMode.System)

    init {
        restore()
    }

    fun restore() {
        updateSessionState(SessionUiState.Bootstrapping)
        scope.launch {
            val state = try {
                val session = restoreSession()
                when {
                    session == null -> SessionUiState.AuthenticationRequired
                    session.profile == null -> SessionUiState.ReconnectRequired
                    else -> SessionUiState.Authenticated(session.profile)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                logger.w { "Session restoration failed" }
                SessionUiState.ReconnectRequired
            }
            updateSessionState(state)
        }
    }

    fun signIn(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            updateSessionState(
                SessionUiState.AuthenticationFailed(AuthFailure.MissingFields)
            )
            return
        }

        updateSessionState(SessionUiState.Authenticating)
        scope.launch {
            val state = try {
                val session = signInUser(
                    AuthCredentials(
                        baseUrl = "https://api.dribl.com/api",
                        path = "/auth/signin",
                        username = username.trim(),
                        password = password,
                    )
                )
                session.profile?.let(SessionUiState::Authenticated)
                    ?: SessionUiState.ReconnectRequired
            } catch (error: CancellationException) {
                throw error
            } catch (error: AuthenticationException) {
                logger.w { "Authentication failed: ${error.reason}" }
                SessionUiState.AuthenticationFailed(
                    when (error.reason) {
                        AuthenticationFailure.InvalidCredentials -> AuthFailure.InvalidCredentials
                        AuthenticationFailure.Offline -> AuthFailure.Offline
                        AuthenticationFailure.Unknown -> AuthFailure.Unknown
                    }
                )
            } catch (_: Exception) {
                logger.e { "Unexpected authentication failure" }
                SessionUiState.AuthenticationFailed(AuthFailure.Unknown)
            }
            updateSessionState(state)
        }
    }

    fun signOut() {
        scope.launch {
            signOutUser()
            updateSessionState(SessionUiState.AuthenticationRequired)
        }
    }

    fun refreshRelatedUsers(email: String) {
        scope.launch {
            refreshProfileEndpoint(ProfileApiEndpoint.RelatedUsers, email.trim())
        }
    }

    fun refreshAccessAccounts() {
        scope.launch {
            refreshProfileEndpoint(ProfileApiEndpoint.AccessAccounts, "")
        }
    }

    fun refreshLinkedUsers() {
        scope.launch {
            refreshProfileEndpoint(ProfileApiEndpoint.LinkedUsers, "")
        }
    }

    fun loadLinkCandidates() {
            mutableLinkMemberState.value = LinkMemberUiState.Loading
            scope.launch {
                mutableLinkMemberState.value = try {
                    LinkMemberUiState.Ready(getLinkCandidates())
                } catch (error: CancellationException) {
                    throw error
                } catch (error: LinkMemberException) {
                    LinkMemberUiState.Failed(error.reason)
                }
            }
        }

    fun verifyLinkedUser(candidateId: String, token: String) {
            val current = mutableLinkMemberState.value as? LinkMemberUiState.Ready ?: return
            if (token.length != LINK_TOKEN_LENGTH || !token.all(Char::isDigit)) return
            mutableLinkMemberState.value = current.copy(verifyingCandidateId = candidateId)
            scope.launch {
                mutableLinkMemberState.value = try {
                    verifyLinkedUserUseCase(candidateId, token)
                    LinkMemberUiState.Linked
                } catch (error: CancellationException) {
                    throw error
                } catch (error: LinkMemberException) {
                    LinkMemberUiState.Failed(error.reason)
            }
        }
    }

    fun updateTheme(mode: ThemeMode) {
        scope.launch { setThemeMode(mode) }
    }

    private fun updateSessionState(state: SessionUiState) {
        mutableSessionState.value = state
        profileObservationJob?.cancel()
        profileObservationJob = null
        val authenticated = state as? SessionUiState.Authenticated ?: return
        profileObservationJob = scope.launch {
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
