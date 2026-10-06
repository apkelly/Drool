package com.github.apkelly.drool.data.weather

import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

actual fun platformWeatherApiKey(): String? =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("GOOGLE_WEATHER_API_KEY") as? String)
        ?.takeIf(String::isNotBlank)

actual fun platformWeatherRequestHeaders(): Map<String, String> =
    NSBundle.mainBundle.bundleIdentifier
        ?.takeIf(String::isNotBlank)
        ?.let { mapOf("X-Ios-Bundle-Identifier" to it) }
        .orEmpty()

actual fun platformWeatherTemperatureUnit(): WeatherTemperatureUnit {
    val usesMetricSystem =
        NSUserDefaults.standardUserDefaults.objectForKey("AppleMetricUnits") as? Boolean
    return if (usesMetricSystem == false) {
        WeatherTemperatureUnit.Fahrenheit
    } else {
        WeatherTemperatureUnit.Celsius
    }
}
