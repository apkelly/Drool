package com.github.apkelly.drool.data.weather

import platform.Foundation.NSBundle

actual fun platformWeatherApiKey(): String? =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("GOOGLE_WEATHER_API_KEY") as? String)
        ?.takeIf(String::isNotBlank)

actual fun platformWeatherRequestHeaders(): Map<String, String> =
    NSBundle.mainBundle.bundleIdentifier
        ?.takeIf(String::isNotBlank)
        ?.let { mapOf("X-Ios-Bundle-Identifier" to it) }
        .orEmpty()
