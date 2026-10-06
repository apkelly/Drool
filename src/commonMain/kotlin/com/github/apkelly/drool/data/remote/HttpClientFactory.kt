package com.github.apkelly.drool.data.remote

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(
    logBodies: Boolean,
    logTag: String,
    loggingEnabled: Boolean = true,
): HttpClient

expect fun platformDefaultBearerToken(): String?
