package com.github.apkelly.drool.profile.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal actual val playerCardMaximumWidth: Dp = 360.dp

@Composable
actual fun rememberPlayerCardTilt(): State<PlayerCardTilt> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(PlayerCardTilt.Zero) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var baselineX: Float? = null
        var baselineY: Float? = null
        var filteredX = 0f
        var filteredY = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(
                event: SensorEvent,
            ) {
                if (baselineX == null || baselineY == null) {
                    filteredX = event.values[0]
                    filteredY = event.values[1]
                    baselineX = filteredX
                    baselineY = filteredY
                    return
                }
                filteredX += SENSOR_FILTER * (event.values[0] - filteredX)
                filteredY += SENSOR_FILTER * (event.values[1] - filteredY)
                val originX = baselineX ?: return
                val originY = baselineY ?: return
                tilt.value = PlayerCardTilt(
                    horizontal = ((filteredX - originX) / TILT_GRAVITY_RANGE)
                        .coerceIn(-1f, 1f),
                    vertical = ((filteredY - originY) / TILT_GRAVITY_RANGE)
                        .coerceIn(-1f, 1f),
                )
            }

            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int,
            ) = Unit
        }

        accelerometer?.let {
            sensorManager.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_GAME,
            )
        }
        onDispose {
            sensorManager.unregisterListener(listener)
            tilt.value = PlayerCardTilt.Zero
        }
    }
    return tilt
}

private const val SENSOR_FILTER = 0.18f
private const val TILT_GRAVITY_RANGE = 4.5f
