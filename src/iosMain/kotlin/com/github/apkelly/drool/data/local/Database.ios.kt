@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.github.apkelly.drool.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun createPlatformDatabaseBuilder(): RoomDatabase.Builder<DroolDatabase> {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )?.path ?: error("Unable to resolve iOS Application Support directory")

    return Room.databaseBuilder<DroolDatabase>(name = "$directory/drool.db")
}
