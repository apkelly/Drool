package com.github.apkelly.drool.common.ui.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
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

    fun register(
        factory: IosGoogleMapViewFactory,
    ) {
        this.factory = factory
    }

    internal fun createMap(
        latitude: Double,
        longitude: Double,
    ): UIView? = factory?.createMap(latitude, longitude)
}

@Composable
actual fun rememberLocationActionLauncher(): LocationActionLauncher = remember {
    object : LocationActionLauncher {
        override fun navigate(
            latitude: Double?,
            longitude: Double?,
            address: String,
        ) {
            val destination = destination(latitude, longitude, address)
            val application = UIApplication.sharedApplication
            val appUrl = buildUrl(
                baseUrl = "comgooglemaps://",
                queryItems = listOf(
                    NSURLQueryItem(name = "daddr", value = destination),
                    NSURLQueryItem(name = "directionsmode", value = "driving"),
                ),
            )
            val webUrl = buildUrl(
                baseUrl = "https://www.google.com/maps/dir/",
                queryItems = listOf(
                    NSURLQueryItem(name = "api", value = "1"),
                    NSURLQueryItem(name = "destination", value = destination),
                    NSURLQueryItem(name = "travelmode", value = "driving"),
                ),
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
): String = if (latitude != null && longitude != null) "$latitude,$longitude" else address

private fun buildUrl(
    baseUrl: String,
    queryItems: List<NSURLQueryItem>,
): NSURL? = NSURLComponents.componentsWithString(baseUrl)?.apply {
    setQueryItems(queryItems)
}?.URL
