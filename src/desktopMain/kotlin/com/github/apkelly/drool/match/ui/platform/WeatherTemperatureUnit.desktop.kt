package com.github.apkelly.drool.match.ui.platform

import java.util.Locale

actual fun platformWeatherTemperatureUnit(): WeatherTemperatureUnit =
    if (Locale.getDefault().country.equals("US", ignoreCase = true)) {
        WeatherTemperatureUnit.Fahrenheit
    } else {
        WeatherTemperatureUnit.Celsius
    }
