package com.github.apkelly.drool.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("clubId")])
data class TeamEntity(
    @PrimaryKey val id: String,
    val clubId: String?,
    val name: String,
    val shortName: String?,
    val logoUrl: String?,
    val ageGroup: String?,
    val competitionName: String?,
    val active: Boolean?,
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
)

@Entity(primaryKeys = ["accountId", "teamId"], indices = [Index("teamId")])
data class TeamRelationshipEntity(
    val accountId: String,
    val teamId: String,
    val relationship: String,
)
