package com.github.apkelly.drool.common.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    primaryKeys = ["accountId", "id", "role"],
    indices = [
        Index("kickoffEpochMillis"),
        Index("userTeamId"),
        Index(value = ["accountId", "kickoffEpochMillis"]),
        Index(value = ["accountId", "role", "kickoffEpochMillis"]),
    ],
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
    val role: String = "Player",
    val refereeRole: String? = null,
    val refereeAllocationId: String? = null,
    val refereeStatus: String? = null,
    val leagueName: String? = null,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val homeTeamLogoUrl: String? = null,
    val awayTeamLogoUrl: String? = null,
    val roundLabel: String? = null,
    val venueAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val leagueId: String? = null,
    val seasonId: String? = null,
    val tenantId: String? = null,
    val matchSheetId: String? = null,
)
