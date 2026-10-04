plugins {
    kotlin("multiplatform") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.1"
    id("com.android.kotlin.multiplatform.library") version "9.4.1"
    id("com.google.devtools.ksp") version "2.3.12"
    id("androidx.room") version "2.8.5"
    id("org.jetbrains.kotlinx.kover") version "0.9.11"
}

group = "com.github.apkelly.drool"
version = "1.0.0"

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
                implementation("org.jetbrains.compose.runtime:runtime:1.12.1")
                implementation("org.jetbrains.compose.foundation:foundation:1.12.1")
                implementation("org.jetbrains.compose.material3:material3:1.9.0")
                implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
                implementation("org.jetbrains.compose.components:components-resources:1.12.1")
                implementation("androidx.datastore:datastore-preferences-core:1.2.1")
                implementation("androidx.room:room-runtime:2.8.5")
                implementation("androidx.sqlite:sqlite-bundled:2.7.1")
                implementation("org.jetbrains.androidx.navigation3:navigation3-ui:1.1.2")
                implementation("io.github.alexzhirkevich:compottie:2.3.2")
                implementation("io.github.alexzhirkevich:compottie-resources:2.3.2")
                implementation("io.coil-kt.coil3:coil-compose:3.6.3")
                implementation("io.coil-kt.coil3:coil-network-ktor3:3.6.3")
                implementation("io.ktor:ktor-client-core:3.6.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.6.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.6.0")
                implementation("io.insert-koin:koin-core:4.2.2")
                implementation("co.touchlab:kermit:2.2.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            }
        }

        getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation("app.cash.turbine:turbine:1.2.1")
                implementation("io.ktor:ktor-client-mock:3.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
            }
        }

        getByName("androidMain") {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:3.6.0")
                implementation("com.squareup.okhttp3:logging-interceptor:5.5.0")
                implementation("com.google.android.gms:play-services-maps:20.0.0")
            }
        }

        getByName("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("io.ktor:ktor-client-okhttp:3.6.0")
                implementation("com.squareup.okhttp3:logging-interceptor:5.5.0")
            }
        }

        val iosMain = create("iosMain") {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-darwin:3.6.0")
            }
        }
        getByName("iosArm64Main") { dependsOn(iosMain) }
        getByName("iosSimulatorArm64Main") { dependsOn(iosMain) }
    }
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.8.5")
    add("kspDesktop", "androidx.room:room-compiler:2.8.5")
    add("kspIosArm64", "androidx.room:room-compiler:2.8.5")
    add("kspIosSimulatorArm64", "androidx.room:room-compiler:2.8.5")
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
