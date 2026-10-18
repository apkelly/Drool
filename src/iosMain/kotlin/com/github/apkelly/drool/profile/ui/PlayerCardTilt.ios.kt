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
private object SharedMotionManager {
    private val manager by lazy { CMMotionManager() }
    private val listeners = linkedSetOf<(Double, Double) -> Unit>()

    fun register(
        listener: (Double, Double) -> Unit,
    ) {
        val shouldStart = listeners.isEmpty()
        listeners.add(listener)
        if (shouldStart && manager.accelerometerAvailable) {
            manager.accelerometerUpdateInterval = SENSOR_UPDATE_SECONDS
            manager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
                data?.acceleration?.let { acceleration ->
                    val (rawX, rawY) = acceleration.useContents { x to y }
                    listeners.toList().forEach { callback ->
                        callback(rawX, rawY)
                    }
                }
            }
        }
    }

    fun unregister(
        listener: (Double, Double) -> Unit,
    ) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            manager.stopAccelerometerUpdates()
        }
    }
}

@Composable
actual fun rememberPlayerCardTilt(): State<PlayerCardTilt> {
    val tilt = remember { mutableStateOf(PlayerCardTilt.Zero) }

    DisposableEffect(Unit) {
        var baselineX: Double? = null
        var baselineY: Double? = null
        var filteredX = 0.0
        var filteredY = 0.0
        val listener: (Double, Double) -> Unit = { rawX, rawY ->
            if (baselineX == null || baselineY == null) {
                filteredX = rawX
                filteredY = rawY
                baselineX = filteredX
                baselineY = filteredY
            } else {
                filteredX += SENSOR_FILTER * (rawX - filteredX)
                filteredY += SENSOR_FILTER * (rawY - filteredY)
                val originX = baselineX
                val originY = baselineY
                if (originX != null && originY != null) {
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
        SharedMotionManager.register(listener)
        onDispose {
            SharedMotionManager.unregister(listener)
            tilt.value = PlayerCardTilt.Zero
        }
    }
    return tilt
}

private const val SENSOR_UPDATE_SECONDS = 1.0 / 30.0
private const val SENSOR_FILTER = 0.18
private const val TILT_GRAVITY_RANGE = 0.45
