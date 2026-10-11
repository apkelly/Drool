package com.github.apkelly.drool.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ProfileEntity(
    @PrimaryKey val accountId: String,
    val displayName: String,
    val email: String?,
    val active: Boolean,
    val avatarUrl: String? = null,
)
