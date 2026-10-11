package com.github.apkelly.drool.common.data.remote

import com.github.apkelly.drool.common.logging.DroolLog
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import okhttp3.logging.HttpLoggingInterceptor

actual fun createPlatformHttpClient(
    logBodies: Boolean,
    logTag: String,
    loggingEnabled: Boolean,
): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            if (loggingEnabled) {
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
                    },
                )
            }
        }
    }
    install(ContentNegotiation) {
        json(networkJson)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = HTTP_TIMEOUT_MILLIS
        connectTimeoutMillis = HTTP_TIMEOUT_MILLIS
        socketTimeoutMillis = HTTP_TIMEOUT_MILLIS
    }
}

actual fun platformDefaultBearerToken(): String? = null
