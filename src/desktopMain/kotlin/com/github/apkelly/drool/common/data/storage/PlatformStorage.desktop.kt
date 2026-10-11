package com.github.apkelly.drool.common.data.storage

import java.io.File

actual fun createPlatformBearerTokenStore(): BearerTokenStore = createBearerTokenStore {
    val directory = File(System.getProperty("user.home"), ".drool")
    directory.mkdirs()
    File(directory, DATA_STORE_FILE_NAME).absolutePath
}
