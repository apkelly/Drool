package com.github.apkelly.drool.common.data.local.entity

import androidx.room.Entity

@Entity(primaryKeys = ["cacheKey", "accountScope"])
data class CacheMetadataEntity(
    val cacheKey: String,
    val accountScope: String,
    val updatedAtEpochMillis: Long,
)
