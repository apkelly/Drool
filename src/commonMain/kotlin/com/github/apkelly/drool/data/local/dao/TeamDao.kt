package com.github.apkelly.drool.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.github.apkelly.drool.data.local.entity.TeamEntity
import com.github.apkelly.drool.data.local.entity.TeamRelationshipEntity

@Dao
interface TeamDao {
    @Query("SELECT * FROM TeamEntity ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM TeamEntity WHERE clubId = :clubId ORDER BY name COLLATE NOCASE")
    fun observeByClub(clubId: String): Flow<List<TeamEntity>>

    @Query("SELECT COUNT(*) FROM TeamEntity")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM TeamEntity WHERE clubId = :clubId")
    suspend fun countForClub(clubId: String): Int

    @Upsert
    suspend fun upsertAll(teams: List<TeamEntity>)

    @Query("DELETE FROM TeamEntity")
    suspend fun deleteAll()

    @Query(
        """
        DELETE FROM TeamEntity
        WHERE clubId = :clubId
          AND id NOT IN (SELECT teamId FROM TeamRelationshipEntity)
        """
    )
    suspend fun deleteUnrelatedForClub(clubId: String)

    @Query("SELECT * FROM TeamRelationshipEntity WHERE accountId = :accountId")
    fun observeRelationships(accountId: String): Flow<List<TeamRelationshipEntity>>

    @Query("SELECT * FROM TeamRelationshipEntity WHERE accountId IN (:accountIds)")
    fun observeRelationships(accountIds: List<String>): Flow<List<TeamRelationshipEntity>>

    @Upsert
    suspend fun upsertRelationship(relationship: TeamRelationshipEntity)

    @Query("DELETE FROM TeamRelationshipEntity WHERE accountId = :accountId AND teamId = :teamId")
    suspend fun deleteRelationship(accountId: String, teamId: String)

    @Query("DELETE FROM TeamRelationshipEntity WHERE accountId = :accountId")
    suspend fun deleteRelationships(accountId: String)

    @Query("DELETE FROM TeamRelationshipEntity")
    suspend fun deleteAllRelationships()
}
