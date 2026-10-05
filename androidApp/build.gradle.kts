import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val localProperties = Properties().apply {
    rootProject.file("local.properties")
        .takeIf { it.isFile }
        ?.inputStream()
        ?.use(::load)
}

android {
    namespace = "com.github.apkelly.drool"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.github.apkelly.drool"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] =
            providers.gradleProperty("GOOGLE_MAPS_API_KEY")
                .orElse(providers.environmentVariable("GOOGLE_MAPS_API_KEY"))
                .orElse(
                    providers.provider {
                        localProperties.getProperty("GOOGLE_MAPS_API_KEY").orEmpty()
                    }
                )
                .orElse("")
                .get()
    }
}

dependencies {
    implementation(project(":"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("io.insert-koin:koin-core:4.2.2")
}
