package com.github.apkelly.drool.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ClubEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shortName: String?,
    val logoUrl: String?,
    val primaryColor: String?,
    val secondaryColor: String?,
)
