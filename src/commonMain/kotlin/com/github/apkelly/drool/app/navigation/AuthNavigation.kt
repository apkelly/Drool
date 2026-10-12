package com.github.apkelly.drool.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.github.apkelly.drool.auth.ui.RegisterScreen
import com.github.apkelly.drool.auth.ui.SignInScreen
import com.github.apkelly.drool.auth.ui.WelcomeScreen
import com.github.apkelly.drool.auth.ui.model.SessionUiState
import com.github.apkelly.drool.common.observability.AppScreen

@Composable
fun AuthNavigation(
    sessionState: SessionUiState,
    onSignIn: (String, String) -> Unit,
    onScreenView: (AppScreen) -> Unit,
) {
    val currentOnScreenView by rememberUpdatedState(onScreenView)
    val backStack = rememberNavBackStack(
        navigationStateConfiguration,
        AppRoute.Welcome as NavKey,
    )
    LaunchedEffect(backStack.lastOrNull()) {
        currentOnScreenView(
            when (backStack.lastOrNull()) {
                AppRoute.SignIn -> AppScreen.SignIn
                AppRoute.Register -> AppScreen.Register
                else -> AppScreen.Welcome
            },
        )
    }
    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        },
        entryProvider = entryProvider {
            entry<AppRoute.Welcome> {
                WelcomeScreen(
                    onSignIn = { backStack.add(AppRoute.SignIn) },
                    onCreateAccount = { backStack.add(AppRoute.Register) },
                )
            }
            entry<AppRoute.SignIn> {
                SignInScreen(
                    loading = sessionState == SessionUiState.Authenticating,
                    failure = (sessionState as? SessionUiState.AuthenticationFailed)?.reason,
                    onBack = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    },
                    onSignIn = onSignIn,
                )
            }
            entry<AppRoute.Register> {
                RegisterScreen(
                    onBack = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    },
                )
            }
        },
    )
}
