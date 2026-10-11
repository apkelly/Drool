package com.github.apkelly.drool.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.apkelly.drool.data.storage.requireAndroidApplicationContext

actual fun createPlatformDatabaseBuilder(): RoomDatabase.Builder<DroolDatabase> {
    val context = requireAndroidApplicationContext()
    return Room.databaseBuilder<DroolDatabase>(
        context = context,
        name = context.getDatabasePath("drool.db").absolutePath,
    )
}
