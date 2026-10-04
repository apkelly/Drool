package com.github.apkelly.drool.ui.platform

import androidx.compose.runtime.Composable

interface ContactActionLauncher {
    fun call(phoneNumber: String)
    fun email(emailAddress: String)
}

@Composable
expect fun rememberContactActionLauncher(): ContactActionLauncher
