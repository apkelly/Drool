package com.github.apkelly.drool.data.storage

import android.content.Context

private lateinit var applicationContext: Context

fun initializeAndroidStorage(context: Context) {
    applicationContext = context.applicationContext
}

internal fun requireAndroidApplicationContext(): Context {
    check(::applicationContext.isInitialized) {
        "Android storage has not been initialized."
    }
    return applicationContext
}

actual fun createPlatformBearerTokenStore(): BearerTokenStore {
    return createBearerTokenStore {
        requireAndroidApplicationContext().filesDir.resolve(DataStoreFileName).absolutePath
    }
}
