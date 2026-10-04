package com.github.apkelly.drool.data.storage

import java.io.File

actual fun createPlatformBearerTokenStore(): BearerTokenStore =
    createBearerTokenStore {
        val directory = File(System.getProperty("user.home"), ".drool")
        directory.mkdirs()
        File(directory, DataStoreFileName).absolutePath
    }
