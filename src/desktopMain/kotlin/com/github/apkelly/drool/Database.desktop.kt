package com.github.apkelly.drool.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual fun createPlatformDatabaseBuilder(): RoomDatabase.Builder<DroolDatabase> {
    val directory = File(System.getProperty("user.home"), ".drool").apply { mkdirs() }
    return Room.databaseBuilder<DroolDatabase>(
        name = File(directory, "drool.db").absolutePath,
    )
}
