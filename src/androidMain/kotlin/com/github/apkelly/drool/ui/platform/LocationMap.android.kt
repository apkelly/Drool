package com.github.apkelly.drool.ui.platform

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

@Composable
actual fun rememberLocationActionLauncher(): LocationActionLauncher {
    val context = LocalContext.current
    return remember(context) {
        object : LocationActionLauncher {
            override fun navigate(
                latitude: Double?,
                longitude: Double?,
                address: String,
            ) {
                val destination = destination(latitude, longitude, address)
                val navigation = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=${Uri.encode(destination)}&mode=d"),
                )
                if (navigation.resolveActivity(context.packageManager) != null) {
                    context.startActivity(navigation)
                } else {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "https://www.google.com/maps/dir/" +
                                    "?api=1&destination=${Uri.encode(destination)}" +
                                    "&travelmode=driving"
                            ),
                        )
                    )
                }
            }
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
    val context = LocalContext.current
    val mapView = remember(context) {
        MapView(context).apply { onCreate(Bundle()) }
    }
    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    LaunchedEffect(mapView, latitude, longitude) {
        if (latitude != null && longitude != null) {
            mapView.getMapAsync { map ->
                val position = LatLng(latitude, longitude)
                map.clear()
                map.uiSettings.setAllGesturesEnabled(false)
                map.addMarker(MarkerOptions().position(position))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
            }
        }
    }
    Box(modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().clickable(onClick = onNavigate))
    }
}

private fun destination(
    latitude: Double?,
    longitude: Double?,
    address: String,
): String =
    if (latitude != null && longitude != null) "$latitude,$longitude" else address
