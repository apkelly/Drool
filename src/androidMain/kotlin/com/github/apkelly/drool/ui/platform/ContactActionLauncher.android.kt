package com.github.apkelly.drool.ui.platform

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberContactActionLauncher(): ContactActionLauncher {
    val context = LocalContext.current
    return remember(context) {
        object : ContactActionLauncher {
            override fun call(phoneNumber: String) {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneNumber.phoneUriValue()}"))
                )
            }

            override fun email(emailAddress: String) {
                context.startActivity(
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$emailAddress"))
                )
            }
        }
    }
}

private fun String.phoneUriValue(): String =
    filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
