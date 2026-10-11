package com.github.apkelly.drool.match.ui.platform

import android.icu.util.LocaleData
import android.icu.util.ULocale
import com.github.apkelly.drool.data.storage.requireAndroidApplicationContext

actual fun platformWeatherTemperatureUnit(): WeatherTemperatureUnit {
    val locale = requireAndroidApplicationContext()
        .resources
        .configuration
        .locales[0]
    return if (
        LocaleData.getMeasurementSystem(ULocale.forLocale(locale)) ==
        LocaleData.MeasurementSystem.US
    ) {
        WeatherTemperatureUnit.Fahrenheit
    } else {
        WeatherTemperatureUnit.Celsius
    }
}
