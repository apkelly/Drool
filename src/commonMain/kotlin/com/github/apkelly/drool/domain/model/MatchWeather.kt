package com.github.apkelly.drool.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MatchWeather(
    val condition: String?,
    val minimumTemperatureCelsius: Double,
    val maximumTemperatureCelsius: Double,
    val relativeHumidityPercent: Int,
    val rainfallProbabilityPercent: Int,
    val rainfallAmountMillimetres: Double,
    val uvIndex: Int,
) {
    val hasElevatedWashoutRisk: Boolean
        get() = rainfallProbabilityPercent >= 70 || rainfallAmountMillimetres >= 10.0

    val sunscreenRecommended: Boolean
        get() = uvIndex >= 3
}
