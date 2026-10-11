package com.github.apkelly.dribl.api

import kotlinx.serialization.json.Json

internal val driblJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}
