package com.github.apkelly.drool.common.data.storage

import android.content.Context

private lateinit var applicationContext: Context

fun initializeAndroidStorage(
    context: Context,
) {
    applicationContext = context.applicationContext
}

internal fun requireAndroidApplicationContext(): Context {
    check(::applicationContext.isInitialized) {
        "Android storage has not been initialized."
    }
    return applicationContext
}

actual fun createPlatformBearerTokenStore(): BearerTokenStore = createBearerTokenStore {
    requireAndroidApplicationContext().filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath
}
