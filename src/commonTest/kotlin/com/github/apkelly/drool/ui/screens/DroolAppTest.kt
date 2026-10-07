package com.github.apkelly.drool.ui.screens

import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.ui.model.SessionUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DroolAppTest {
    @Test
    fun startupWaitsForBothAnimationAndSessionRestoration() {
        val authenticated = SessionUiState.Authenticated(
            Profile("account", "Andrew", null)
        )

        assertTrue(shouldShowInitialSplash(false, authenticated))
        assertTrue(
            shouldShowInitialSplash(
                true,
                SessionUiState.Bootstrapping,
            )
        )
        assertFalse(shouldShowInitialSplash(true, authenticated))
        assertFalse(
            shouldShowInitialSplash(
                true,
                SessionUiState.AuthenticationRequired,
            )
        )
    }
}
