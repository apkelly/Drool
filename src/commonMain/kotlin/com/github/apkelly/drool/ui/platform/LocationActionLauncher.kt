package com.github.apkelly.drool.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface LocationActionLauncher {
    fun navigate(
        latitude: Double?,
        longitude: Double?,
        address: String,
    )
}

@Composable
expect fun rememberLocationActionLauncher(): LocationActionLauncher

@Composable
expect fun GoogleMapPreview(
    latitude: Double?,
    longitude: Double?,
    address: String,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
)
