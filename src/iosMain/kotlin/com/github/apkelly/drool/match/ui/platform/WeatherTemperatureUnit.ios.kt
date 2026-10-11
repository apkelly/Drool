package com.github.apkelly.drool.match.ui.platform

import platform.Foundation.NSUserDefaults

actual fun platformWeatherTemperatureUnit(): WeatherTemperatureUnit {
    val usesMetricSystem =
        NSUserDefaults.standardUserDefaults.objectForKey("AppleMetricUnits") as? Boolean
    return if (usesMetricSystem == false) {
        WeatherTemperatureUnit.Fahrenheit
    } else {
        WeatherTemperatureUnit.Celsius
    }
}
