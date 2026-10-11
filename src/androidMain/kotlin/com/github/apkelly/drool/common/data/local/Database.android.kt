package com.github.apkelly.drool.common.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.apkelly.drool.common.data.storage.requireAndroidApplicationContext

actual fun createPlatformDatabaseBuilder(): RoomDatabase.Builder<DroolDatabase> {
    val context = requireAndroidApplicationContext()
    return Room.databaseBuilder<DroolDatabase>(
        context = context,
        name = context.getDatabasePath("drool.db").absolutePath,
    )
}
