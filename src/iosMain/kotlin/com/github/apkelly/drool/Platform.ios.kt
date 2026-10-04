package com.github.apkelly.drool.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

actual fun createPlatformHttpClient(
    logBodies: Boolean,
    logTag: String,
): HttpClient =
    HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(networkJson)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }

actual fun platformDefaultBearerToken(): String? = null
