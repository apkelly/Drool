package com.github.apkelly.drool.ui.platform

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIView

interface IosGoogleMapViewFactory {
    fun createMap(
        latitude: Double,
        longitude: Double,
    ): UIView
}

object IosGoogleMapRegistry {
    private var factory: IosGoogleMapViewFactory? = null

    fun register(factory: IosGoogleMapViewFactory) {
        this.factory = factory
    }

    internal fun createMap(
        latitude: Double,
        longitude: Double,
    ): UIView? = factory?.createMap(latitude, longitude)
}

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
                val application = UIApplication.sharedApplication
                val appUrl = NSURL.URLWithString(
                    "comgooglemaps://?daddr=${destination.urlQueryValue()}" +
                        "&directionsmode=driving"
                )
                val webUrl = NSURL.URLWithString(
                    "https://www.google.com/maps/dir/" +
                        "?api=1&destination=${destination.urlQueryValue()}" +
                        "&travelmode=driving"
                )
                when {
                    appUrl != null && application.canOpenURL(appUrl) ->
                        application.openURL(appUrl)
                    webUrl != null -> application.openURL(webUrl)
                }
            }
        }
    }

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun GoogleMapPreview(
    latitude: Double?,
    longitude: Double?,
    address: String,
    onNavigate: () -> Unit,
    modifier: Modifier,
) {
    val mapView = remember(latitude, longitude) {
        if (latitude != null && longitude != null) {
            IosGoogleMapRegistry.createMap(latitude, longitude)
        } else {
            null
        }
    }
    Box(modifier) {
        if (mapView != null) {
            UIKitView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                properties = UIKitInteropProperties(
                    isInteractive = false,
                    isNativeAccessibilityEnabled = false,
                ),
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color.LightGray))
        }
        Box(Modifier.fillMaxSize().clickable(onClick = onNavigate))
    }
}

private fun destination(
    latitude: Double?,
    longitude: Double?,
    address: String,
): String =
    if (latitude != null && longitude != null) "$latitude,$longitude" else address

private fun String.urlQueryValue(): String =
    replace("%", "%25")
        .replace(" ", "%20")
        .replace("#", "%23")
        .replace("&", "%26")
        .replace(",", "%2C")
