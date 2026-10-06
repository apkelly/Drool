package com.github.apkelly.drool.data.weather

import android.os.Build
import android.content.pm.PackageManager
import android.icu.util.LocaleData
import android.icu.util.ULocale
import java.security.MessageDigest
import com.github.apkelly.drool.data.storage.requireAndroidApplicationContext

actual fun platformWeatherApiKey(): String? {
    val context = requireAndroidApplicationContext()
    val applicationInfo = context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA,
    )
    return applicationInfo.metaData
        ?.getString("com.github.apkelly.drool.WEATHER_API_KEY")
        ?.takeIf(String::isNotBlank)
}

actual fun platformWeatherRequestHeaders(): Map<String, String> {
    val context = requireAndroidApplicationContext()
    val certificate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES,
        ).signingInfo?.apkContentsSigners?.firstOrNull()
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES,
        ).signatures?.firstOrNull()
    } ?: return emptyMap()
    val fingerprint = MessageDigest.getInstance("SHA-1")
        .digest(certificate.toByteArray())
        .joinToString(separator = "") { byte -> "%02X".format(byte) }
    return mapOf(
        "X-Android-Package" to context.packageName,
        "X-Android-Cert" to fingerprint,
    )
}

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
