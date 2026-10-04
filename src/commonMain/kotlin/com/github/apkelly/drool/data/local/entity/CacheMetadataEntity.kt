package com.github.apkelly.drool.data.local.entity

import androidx.room.Entity

@Entity(primaryKeys = ["cacheKey", "accountScope"])
data class CacheMetadataEntity(
    val cacheKey: String,
    val accountScope: String,
    val updatedAtEpochMillis: Long,
)
