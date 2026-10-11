package com.github.apkelly.drool.app.ui

import com.github.apkelly.drool.auth.ui.model.SessionUiState
import com.github.apkelly.drool.common.domain.model.Profile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DroolAppTest {
    @Test
    fun startupWaitsForBothAnimationAndSessionRestoration() {
        val authenticated = SessionUiState.Authenticated(
            Profile("account", "Harry Kane", null),
        )

        assertTrue(shouldShowInitialSplash(false, authenticated))
        assertTrue(
            shouldShowInitialSplash(
                true,
                SessionUiState.Bootstrapping,
            ),
        )
        assertFalse(shouldShowInitialSplash(true, authenticated))
        assertFalse(
            shouldShowInitialSplash(
                true,
                SessionUiState.AuthenticationRequired,
            ),
        )
    }
}
