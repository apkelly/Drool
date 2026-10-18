package com.github.apkelly.drool.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.github.apkelly.drool.app.navigation.AuthNavigation
import com.github.apkelly.drool.app.navigation.MainNavigation
import com.github.apkelly.drool.app.ui.viewmodel.AppViewModel
import com.github.apkelly.drool.auth.ui.ReconnectScreen
import com.github.apkelly.drool.auth.ui.model.SessionUiState
import com.github.apkelly.drool.common.ui.widgets.SplashContent
import com.github.apkelly.drool.discover.ui.viewmodel.DiscoverViewModel
import com.github.apkelly.drool.match.ui.viewmodel.MatchViewModel
import com.github.apkelly.drool.schedule.ui.viewmodel.AvailabilityViewModel
import com.github.apkelly.drool.schedule.ui.viewmodel.ScheduleViewModel
import com.github.apkelly.drool.team.ui.viewmodel.TeamViewModel

@Composable
fun DroolApp(
    appViewModel: AppViewModel,
    discoverViewModel: DiscoverViewModel,
    scheduleViewModel: ScheduleViewModel,
    teamViewModel: TeamViewModel,
    matchViewModel: MatchViewModel,
    availabilityViewModel: AvailabilityViewModel,
) {
    val sessionState by appViewModel.sessionState.collectAsState()
    var initialSplashFinished by rememberSaveable { mutableStateOf(false) }
    val authenticatedProfile =
        (sessionState as? SessionUiState.Authenticated)?.profile
    if (authenticatedProfile != null) {
        LaunchedEffect(authenticatedProfile.accountId, authenticatedProfile.linkedProfiles) {
            scheduleViewModel.configureLinkedProfiles(authenticatedProfile)
            teamViewModel.configureAccount(authenticatedProfile.accountId)
            matchViewModel.configureAccount(authenticatedProfile.accountId)
            availabilityViewModel.configureLinkedProfiles(
                accountId = authenticatedProfile.accountId,
                profiles = authenticatedProfile.linkedProfiles,
            )
            discoverViewModel.refreshClubs(force = false)
            scheduleViewModel.refreshFixtures(force = false)
        }
    } else {
        LaunchedEffect(Unit) {
            discoverViewModel.selectClub(null)
            scheduleViewModel.selectLinkedProfile(null)
        }
    }
    if (shouldShowInitialSplash(initialSplashFinished, sessionState)) {
        SplashContent(onAnimationFinish = { initialSplashFinished = true })
        return
    }
    when (val state = sessionState) {
        SessionUiState.Bootstrapping -> Unit

        SessionUiState.ReconnectRequired -> ReconnectScreen(appViewModel::restore)

        SessionUiState.AuthenticationRequired,
        SessionUiState.Authenticating,
        is SessionUiState.AuthenticationFailed,
        -> AuthNavigation(
            sessionState = state,
            onSignIn = appViewModel::signIn,
            onScreenView = appViewModel::trackScreen,
        )

        is SessionUiState.Authenticated -> MainNavigation(
            profile = state.profile,
            appViewModel = appViewModel,
            discoverViewModel = discoverViewModel,
            scheduleViewModel = scheduleViewModel,
            teamViewModel = teamViewModel,
            matchViewModel = matchViewModel,
            availabilityViewModel = availabilityViewModel,
        )
    }
}

internal fun shouldShowInitialSplash(
    animationFinished: Boolean,
    sessionState: SessionUiState,
): Boolean = !animationFinished || sessionState == SessionUiState.Bootstrapping
