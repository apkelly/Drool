package com.github.apkelly.drool.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberContactActionLauncher(): ContactActionLauncher =
    remember {
        object : ContactActionLauncher {
            override fun call(phoneNumber: String) {
                open("tel:${phoneNumber.phoneUriValue()}")
            }

            override fun email(emailAddress: String) {
                open("mailto:$emailAddress")
            }

            private fun open(uri: String) {
                val url = NSURL.URLWithString(uri) ?: return
                UIApplication.sharedApplication.openURL(url)
            }
        }
    }

private fun String.phoneUriValue(): String =
    filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
