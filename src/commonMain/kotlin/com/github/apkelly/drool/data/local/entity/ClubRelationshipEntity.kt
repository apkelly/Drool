package com.github.apkelly.drool.data.local.entity

import androidx.room.Entity

@Entity(primaryKeys = ["accountId", "clubId"])
data class ClubRelationshipEntity(
    val accountId: String,
    val clubId: String,
)
