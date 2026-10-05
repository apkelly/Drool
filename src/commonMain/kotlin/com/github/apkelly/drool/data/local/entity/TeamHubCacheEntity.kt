package com.github.apkelly.drool.data.local.entity

import androidx.room.Entity

@Entity(primaryKeys = ["profileId", "teamId"])
data class TeamHubCacheEntity(
    val profileId: String,
    val teamId: String,
    val payloadJson: String,
    val updatedAtEpochMillis: Long,
)
