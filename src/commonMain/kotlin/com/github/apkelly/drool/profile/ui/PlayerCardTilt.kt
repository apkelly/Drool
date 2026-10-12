package com.github.apkelly.drool.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp

@Immutable
data class PlayerCardTilt(
    val horizontal: Float,
    val vertical: Float,
) {
    companion object {
        val Zero = PlayerCardTilt(0f, 0f)
    }
}

@Composable
expect fun rememberPlayerCardTilt(): State<PlayerCardTilt>

internal expect val playerCardMaximumWidth: Dp
