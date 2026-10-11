package com.github.apkelly.drool.common.data.remote

import io.ktor.client.HttpClient

internal const val HTTP_TIMEOUT_MILLIS = 30_000L

expect fun createPlatformHttpClient(
    logBodies: Boolean,
    logTag: String,
    loggingEnabled: Boolean = true,
): HttpClient

expect fun platformDefaultBearerToken(): String?
