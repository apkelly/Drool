package com.github.apkelly.drool.ui.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.match_google_maps
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun rememberLocationActionLauncher(): LocationActionLauncher =
    remember {
        object : LocationActionLauncher {
            override fun navigate(
                latitude: Double?,
                longitude: Double?,
                address: String,
            ) {
                val destination = destination(latitude, longitude, address)
                Desktop.getDesktop().browse(
                    URI(
                        "https://www.google.com/maps/dir/" +
                            "?api=1&destination=${destination.urlQueryValue()}" +
                            "&travelmode=driving"
                    )
                )
            }
        }
    }

@Composable
actual fun GoogleMapPreview(
    latitude: Double?,
    longitude: Double?,
    address: String,
    onNavigate: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onNavigate),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null)
            Text(stringResource(Res.string.match_google_maps))
        }
    }
}

private fun destination(
    latitude: Double?,
    longitude: Double?,
    address: String,
): String =
    if (latitude != null && longitude != null) "$latitude,$longitude" else address

private fun String.urlQueryValue(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
