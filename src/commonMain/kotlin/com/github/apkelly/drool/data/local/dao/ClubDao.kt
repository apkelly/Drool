package com.github.apkelly.drool.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.github.apkelly.drool.data.local.entity.ClubEntity
import com.github.apkelly.drool.data.local.entity.ClubRelationshipEntity

@Dao
interface ClubDao {
    @Query("SELECT * FROM ClubEntity ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ClubEntity>>

    @Query("SELECT COUNT(*) FROM ClubEntity")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(clubs: List<ClubEntity>)

    @Query("SELECT * FROM ClubRelationshipEntity WHERE accountId IN (:accountIds)")
    fun observeRelationships(accountIds: List<String>): Flow<List<ClubRelationshipEntity>>

    @Upsert
    suspend fun upsertRelationships(relationships: List<ClubRelationshipEntity>)

    @Query("DELETE FROM ClubRelationshipEntity WHERE accountId = :accountId")
    suspend fun deleteRelationships(accountId: String)

    @Query("DELETE FROM ClubRelationshipEntity")
    suspend fun deleteAllRelationships()

    @Query("DELETE FROM ClubEntity")
    suspend fun deleteAll()
}
