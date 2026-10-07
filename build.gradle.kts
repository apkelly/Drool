import java.util.Properties
import org.gradle.api.tasks.JavaExec

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kover)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

group = "com.github.apkelly.drool"
version = "1.0.0"

val localProperties = Properties().apply {
    file("local.properties")
        .takeIf { it.isFile }
        ?.inputStream()
        ?.use(::load)
}
val desktopMapsApiKey = providers.gradleProperty("GOOGLE_MAPS_STATIC_API_KEY")
    .orElse(providers.environmentVariable("GOOGLE_MAPS_STATIC_API_KEY"))
    .orElse(
        providers.provider {
            localProperties.getProperty("GOOGLE_MAPS_STATIC_API_KEY").orEmpty()
        }
    )
val configuredDesktopWeatherApiKey = providers.gradleProperty("GOOGLE_WEATHER_API_KEY")
    .orElse(providers.environmentVariable("GOOGLE_WEATHER_API_KEY"))
    .orElse(
        providers.provider {
            localProperties.getProperty("GOOGLE_WEATHER_API_KEY").orEmpty()
        }
    )
val desktopWeatherApiKey = configuredDesktopWeatherApiKey
    .map { it.ifBlank { desktopMapsApiKey.get() } }
    .orElse(desktopMapsApiKey)

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.github.apkelly.drool.shared"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
        androidResources {
            enable = true
        }
    }
    jvm("desktop") {
        mainRun {
            mainClass.set("com.github.apkelly.drool.desktop.MainKt")
        }
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "DroolShared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.components.resources)
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.navigation3.ui)
                implementation(libs.compottie)
                implementation(libs.compottie.dot)
                implementation(libs.compottie.resources)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.koin.core)
                implementation(libs.kermit)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        getByName("commonTest") {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.turbine)
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        getByName("androidMain") {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.okhttp.logging)
                implementation(libs.google.maps)
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.analytics)
                implementation(libs.firebase.crashlytics)
            }
        }

        getByName("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.okhttp.logging)
            }
        }

        val iosMain = create("iosMain") {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        getByName("iosArm64Main") { dependsOn(iosMain) }
        getByName("iosSimulatorArm64Main") { dependsOn(iosMain) }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

compose.desktop {
    application {
        mainClass = "com.github.apkelly.drool.desktop.MainKt"
        nativeDistributions {
            packageName = "Drool"
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    doFirst {
        desktopMapsApiKey.orNull
            ?.takeIf { it.isNotBlank() }
            ?.let { systemProperty("GOOGLE_MAPS_STATIC_API_KEY", it) }
        desktopWeatherApiKey.orNull
            ?.takeIf { it.isNotBlank() }
            ?.let { systemProperty("GOOGLE_WEATHER_API_KEY", it) }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.github.apkelly.drool.resources"
}

kover {
    reports {
        filters {
            includes {
                classes(
                    "com.github.apkelly.drool.domain.usecase.*",
                    "com.github.apkelly.drool.data.repository.*",
                )
            }
        }
        verify {
            rule("Use cases and repositories line coverage") {
                minBound(100)
            }
            rule("Use cases and repositories branch coverage") {
                minBound(
                    100,
                    kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH,
                    kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE,
                )
            }
        }
    }
}
