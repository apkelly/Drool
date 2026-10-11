@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.github.apkelly.drool.common.data.storage

import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun createPlatformBearerTokenStore(): BearerTokenStore = createBearerTokenStore {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )?.path ?: error("Unable to resolve iOS Application Support directory")

    "$directory/$DATA_STORE_FILE_NAME"
}
