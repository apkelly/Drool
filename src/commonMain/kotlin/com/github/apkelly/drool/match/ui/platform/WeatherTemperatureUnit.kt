package com.github.apkelly.drool.match.ui.platform

expect fun platformWeatherTemperatureUnit(): WeatherTemperatureUnit

enum class WeatherTemperatureUnit(
    val symbol: String,
) {
    Celsius("°C"),
    Fahrenheit("°F"),
    ;

    fun fromCelsius(
        value: Double,
    ): Double = when (this) {
        Celsius -> value

        Fahrenheit ->
            value * FAHRENHEIT_SCALE_NUMERATOR /
                FAHRENHEIT_SCALE_DENOMINATOR + FAHRENHEIT_FREEZING_POINT
    }

    private companion object {
        const val FAHRENHEIT_SCALE_NUMERATOR = 9.0
        const val FAHRENHEIT_SCALE_DENOMINATOR = 5.0
        const val FAHRENHEIT_FREEZING_POINT = 32.0
    }
}
