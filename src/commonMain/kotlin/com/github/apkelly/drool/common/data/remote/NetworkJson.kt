package com.github.apkelly.drool.common.data.remote

import kotlinx.serialization.json.Json

internal val networkJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}
