package com.github.apkelly.drool.match.ui.platform

expect fun platformWeatherTemperatureUnit(): WeatherTemperatureUnit

enum class WeatherTemperatureUnit(val symbol: String) {
    Celsius("°C"),
    Fahrenheit("°F"),
    ;

    fun fromCelsius(value: Double): Double = when (this) {
        Celsius -> value
        Fahrenheit -> value * 9.0 / 5.0 + 32.0
    }
}
