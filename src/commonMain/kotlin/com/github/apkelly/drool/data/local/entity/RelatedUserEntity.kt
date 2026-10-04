package com.github.apkelly.drool.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["ownerAccountId", "id"])
data class RelatedUserEntity(
    val ownerAccountId: String,
    val id: String,
    val displayName: String,
    val email: String?,
    val avatarUrl: String?,
    @ColumnInfo(defaultValue = "0")
    val isLinked: Boolean = false,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val emergencyContactsJson: String? = null,
)
