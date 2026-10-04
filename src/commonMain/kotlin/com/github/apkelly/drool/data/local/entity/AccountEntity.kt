package com.github.apkelly.drool.data.local.entity

import androidx.room.Entity

@Entity(primaryKeys = ["ownerAccountId", "id"])
data class AccountEntity(
    val ownerAccountId: String,
    val id: String,
    val name: String,
    val subtitle: String?,
    val logoUrl: String?,
)
