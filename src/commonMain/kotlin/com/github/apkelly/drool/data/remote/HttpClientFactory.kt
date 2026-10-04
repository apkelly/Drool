package com.github.apkelly.drool.data.remote

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(
    logBodies: Boolean,
    logTag: String,
): HttpClient

expect fun platformDefaultBearerToken(): String?
