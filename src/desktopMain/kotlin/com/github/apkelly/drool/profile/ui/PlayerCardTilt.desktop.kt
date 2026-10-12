package com.github.apkelly.drool.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal actual val playerCardMaximumWidth: Dp = 280.dp

@Composable
actual fun rememberPlayerCardTilt(): State<PlayerCardTilt> = remember {
    mutableStateOf(PlayerCardTilt.Zero)
}
