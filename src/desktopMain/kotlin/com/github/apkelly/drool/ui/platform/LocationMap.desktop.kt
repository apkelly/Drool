package com.github.apkelly.drool.ui.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.match_google_maps
import com.github.apkelly.drool.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.ui.widgets.MaterialSymbolIcon
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
    val apiKey = remember {
        System.getProperty(DesktopStaticMapsApiKey)
            ?.takeIf { it.isNotBlank() }
            ?: System.getenv(DesktopStaticMapsApiKey)?.takeIf { it.isNotBlank() }
    }
    val mapUrl = remember(latitude, longitude, address, apiKey) {
        apiKey?.let {
            staticMapUrl(
                latitude = latitude,
                longitude = longitude,
                address = address,
                apiKey = it,
            )
        }
    }
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
            MaterialSymbolIcon(MaterialSymbol.LocationOn, contentDescription = null)
            Text(stringResource(Res.string.match_google_maps))
        }
        mapUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = stringResource(Res.string.match_google_maps),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
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

internal fun staticMapUrl(
    latitude: Double?,
    longitude: Double?,
    address: String,
    apiKey: String,
): String {
    val location = destination(latitude, longitude, address).urlQueryValue()
    return "https://maps.googleapis.com/maps/api/staticmap" +
        "?center=$location" +
        "&zoom=15" +
        "&size=640x320" +
        "&scale=2" +
        "&maptype=roadmap" +
        "&markers=color%3Ared%7C$location" +
        "&key=${apiKey.urlQueryValue()}"
}

private const val DesktopStaticMapsApiKey = "GOOGLE_MAPS_STATIC_API_KEY"
