import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties")
        .takeIf { it.isFile }
        ?.inputStream()
        ?.use(::load)
}
val mapsApiKey = providers.gradleProperty("GOOGLE_MAPS_API_KEY")
    .orElse(providers.environmentVariable("GOOGLE_MAPS_API_KEY"))
    .orElse(
        providers.provider {
            localProperties.getProperty("GOOGLE_MAPS_API_KEY").orEmpty()
        }
    )
    .orElse("")
val configuredWeatherApiKey = providers.gradleProperty("GOOGLE_WEATHER_API_KEY")
    .orElse(providers.environmentVariable("GOOGLE_WEATHER_API_KEY"))
    .orElse(
        providers.provider {
            localProperties.getProperty("GOOGLE_WEATHER_API_KEY").orEmpty()
        }
    )
val weatherApiKey = configuredWeatherApiKey
    .map { it.ifBlank { mapsApiKey.get() } }
    .orElse(mapsApiKey)

android {
    namespace = "com.github.apkelly.drool"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.github.apkelly.drool"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = mapsApiKey.get()
        manifestPlaceholders["GOOGLE_WEATHER_API_KEY"] = weatherApiKey.get()
    }
}

dependencies {
    implementation(project(":"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.core)
}
