package com.github.apkelly.drool.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberContactActionLauncher(): ContactActionLauncher =
    remember {
        object : ContactActionLauncher {
            override fun call(phoneNumber: String) {
                Desktop.getDesktop().browse(
                    URI("tel:${phoneNumber.phoneUriValue()}")
                )
            }

            override fun email(emailAddress: String) {
                Desktop.getDesktop().mail(URI("mailto:$emailAddress"))
            }
        }
    }

private fun String.phoneUriValue(): String =
    filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
