package com.github.apkelly.drool.common.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.github.apkelly.drool.common.data.local.entity.TeamEntity
import com.github.apkelly.drool.common.data.local.entity.TeamRelationshipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Query("SELECT * FROM TeamEntity ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM TeamEntity WHERE clubId = :clubId ORDER BY name COLLATE NOCASE")
    fun observeByClub(
        clubId: String,
    ): Flow<List<TeamEntity>>

    @Query("SELECT COUNT(*) FROM TeamEntity")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM TeamEntity WHERE clubId = :clubId")
    suspend fun countForClub(
        clubId: String,
    ): Int

    @Upsert
    suspend fun upsertAll(
        teams: List<TeamEntity>,
    )

    @Query("DELETE FROM TeamEntity")
    suspend fun deleteAll()

    @Query(
        """
        DELETE FROM TeamEntity
        WHERE clubId = :clubId
          AND id NOT IN (SELECT teamId FROM TeamRelationshipEntity)
        """,
    )
    suspend fun deleteUnrelatedForClub(
        clubId: String,
    )

    @Query(
        """
        SELECT * FROM TeamEntity
        WHERE id IN (
            SELECT teamId FROM TeamRelationshipEntity WHERE accountId IN (:accountIds)
        )
        ORDER BY name COLLATE NOCASE
        """,
    )
    fun observeForAccounts(
        accountIds: List<String>,
    ): Flow<List<TeamEntity>>

    @Query("SELECT * FROM TeamRelationshipEntity WHERE accountId = :accountId")
    fun observeRelationships(
        accountId: String,
    ): Flow<List<TeamRelationshipEntity>>

    @Query("SELECT * FROM TeamRelationshipEntity WHERE accountId IN (:accountIds)")
    fun observeRelationships(
        accountIds: List<String>,
    ): Flow<List<TeamRelationshipEntity>>

    @Upsert
    suspend fun upsertRelationship(
        relationship: TeamRelationshipEntity,
    )

    @Upsert
    suspend fun upsertRelationships(
        relationships: List<TeamRelationshipEntity>,
    )

    @Query("DELETE FROM TeamRelationshipEntity WHERE accountId = :accountId AND teamId = :teamId")
    suspend fun deleteRelationship(
        accountId: String,
        teamId: String,
    )

    @Query(
        """
        DELETE FROM TeamRelationshipEntity
        WHERE accountId = :accountId
          AND relationship != :preservedRelationship
        """,
    )
    suspend fun deleteServerRelationships(
        accountId: String,
        preservedRelationship: String = "Following",
    )

    @Query("DELETE FROM TeamRelationshipEntity WHERE accountId = :accountId")
    suspend fun deleteRelationships(
        accountId: String,
    )

    @Query("DELETE FROM TeamRelationshipEntity")
    suspend fun deleteAllRelationships()
}
