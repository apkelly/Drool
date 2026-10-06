package com.github.apkelly.drool.data.weather

import java.util.Locale

actual fun platformWeatherApiKey(): String? =
    sequenceOf(
        System.getProperty("GOOGLE_WEATHER_API_KEY"),
        System.getenv("GOOGLE_WEATHER_API_KEY"),
        System.getProperty("GOOGLE_MAPS_STATIC_API_KEY"),
        System.getenv("GOOGLE_MAPS_STATIC_API_KEY"),
    ).firstOrNull { !it.isNullOrBlank() }

actual fun platformWeatherRequestHeaders(): Map<String, String> = emptyMap()

actual fun platformWeatherTemperatureUnit(): WeatherTemperatureUnit =
    if (Locale.getDefault().country.equals("US", ignoreCase = true)) {
        WeatherTemperatureUnit.Fahrenheit
    } else {
        WeatherTemperatureUnit.Celsius
    }
