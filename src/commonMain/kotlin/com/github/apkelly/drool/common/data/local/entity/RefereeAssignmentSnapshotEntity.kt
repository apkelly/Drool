package com.github.apkelly.drool.common.data.local.entity

import androidx.room.Entity

@Entity(primaryKeys = ["ownerAccountId", "assignmentKey"])
data class RefereeAssignmentSnapshotEntity(
    val ownerAccountId: String,
    val assignmentKey: String,
    val profileId: String,
    val fixtureId: String,
    val allocationId: String?,
    val refereeStatus: String?,
    val homeTeamName: String,
    val awayTeamName: String,
)
