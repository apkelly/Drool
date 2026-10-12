package com.github.apkelly.drool.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

internal actual val playerCardMaximumWidth: Dp = 360.dp

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberPlayerCardTilt(): State<PlayerCardTilt> {
    val tilt = remember { mutableStateOf(PlayerCardTilt.Zero) }
    val motionManager = remember { CMMotionManager() }

    DisposableEffect(motionManager) {
        var baselineX: Double? = null
        var baselineY: Double? = null
        var filteredX = 0.0
        var filteredY = 0.0
        if (motionManager.accelerometerAvailable) {
            motionManager.accelerometerUpdateInterval = SENSOR_UPDATE_SECONDS
            motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) {
                    data,
                    _,
                ->
                data?.acceleration?.let { acceleration ->
                    val (rawX, rawY) = acceleration.useContents { x to y }
                    if (baselineX == null || baselineY == null) {
                        filteredX = rawX
                        filteredY = rawY
                        baselineX = filteredX
                        baselineY = filteredY
                        return@let
                    }
                    filteredX += SENSOR_FILTER * (rawX - filteredX)
                    filteredY += SENSOR_FILTER * (rawY - filteredY)
                    val originX = baselineX
                    val originY = baselineY
                    if (originX == null || originY == null) {
                        return@let
                    }
                    tilt.value = PlayerCardTilt(
                        horizontal = ((filteredX - originX) / TILT_GRAVITY_RANGE)
                            .toFloat()
                            .coerceIn(-1f, 1f),
                        vertical = ((filteredY - originY) / TILT_GRAVITY_RANGE)
                            .toFloat()
                            .coerceIn(-1f, 1f),
                    )
                }
            }
        }
        onDispose {
            motionManager.stopAccelerometerUpdates()
            tilt.value = PlayerCardTilt.Zero
        }
    }
    return tilt
}

private const val SENSOR_UPDATE_SECONDS = 1.0 / 30.0
private const val SENSOR_FILTER = 0.18
private const val TILT_GRAVITY_RANGE = 0.45
