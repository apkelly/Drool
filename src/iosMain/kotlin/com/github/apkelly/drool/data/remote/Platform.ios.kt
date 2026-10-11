package com.github.apkelly.drool.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

actual fun createPlatformHttpClient(
    logBodies: Boolean,
    logTag: String,
    loggingEnabled: Boolean,
): HttpClient = HttpClient(Darwin) {
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
