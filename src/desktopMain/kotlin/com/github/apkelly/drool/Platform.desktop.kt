package com.github.apkelly.drool.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import com.github.apkelly.drool.logging.DroolLog
import okhttp3.logging.HttpLoggingInterceptor

actual fun createPlatformHttpClient(
    logBodies: Boolean,
    logTag: String,
): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                addInterceptor(
                    HttpLoggingInterceptor { message ->
                        DroolLog.withTag(logTag).d {
                            redactSensitiveHttpLogMessage(message)
                        }
                    }.apply {
                        level = if (logBodies) {
                            HttpLoggingInterceptor.Level.BODY
                        } else {
                            HttpLoggingInterceptor.Level.HEADERS
                        }
                        redactHeader("Authorization")
                        redactHeader("Cookie")
                        redactHeader("Set-Cookie")
                    }
                )
            }
        }
        install(ContentNegotiation) {
            json(networkJson)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }

actual fun platformDefaultBearerToken(): String? =
    System.getenv("DRIBL_BEARER_TOKEN")?.takeIf { it.isNotBlank() }
