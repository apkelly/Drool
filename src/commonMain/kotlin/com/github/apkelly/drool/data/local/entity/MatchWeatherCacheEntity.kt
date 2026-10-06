package com.github.apkelly.drool.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MatchWeatherCacheEntity(
    @PrimaryKey val matchId: String,
    val payloadJson: String?,
    val updatedAtEpochMillis: Long,
)
