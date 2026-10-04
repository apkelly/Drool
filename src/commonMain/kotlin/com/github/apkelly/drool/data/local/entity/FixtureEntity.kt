package com.github.apkelly.drool.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    primaryKeys = ["accountId", "id"],
    indices = [Index("kickoffEpochMillis"), Index("userTeamId")],
)
data class FixtureEntity(
    val id: String,
    val accountId: String,
    val kickoffEpochMillis: Long,
    val homeTeamId: String?,
    val homeTeamName: String,
    val awayTeamId: String?,
    val awayTeamName: String,
    val competitionName: String?,
    val venueName: String?,
    val userTeamId: String?,
    val status: String,
)
