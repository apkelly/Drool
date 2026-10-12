package com.github.apkelly.drool.common.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.github.apkelly.drool.common.data.local.entity.RefereeAssignmentSnapshotEntity

@Dao
interface RefereeAssignmentSnapshotDao {
    @Query(
        "SELECT * FROM RefereeAssignmentSnapshotEntity " +
            "WHERE ownerAccountId = :ownerAccountId ORDER BY assignmentKey",
    )
    suspend fun getForOwner(
        ownerAccountId: String,
    ): List<RefereeAssignmentSnapshotEntity>

    @Upsert
    suspend fun upsertAll(
        assignments: List<RefereeAssignmentSnapshotEntity>,
    )

    @Query("DELETE FROM RefereeAssignmentSnapshotEntity WHERE ownerAccountId = :ownerAccountId")
    suspend fun deleteForOwner(
        ownerAccountId: String,
    )

    @Query("DELETE FROM RefereeAssignmentSnapshotEntity")
    suspend fun deleteAll()
}
