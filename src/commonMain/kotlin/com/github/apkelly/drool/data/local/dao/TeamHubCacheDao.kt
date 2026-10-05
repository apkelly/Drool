package com.github.apkelly.drool.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.github.apkelly.drool.data.local.entity.TeamHubCacheEntity

@Dao
interface TeamHubCacheDao {
    @Query(
        """
        SELECT * FROM TeamHubCacheEntity
        WHERE profileId = :profileId AND teamId = :teamId
        LIMIT 1
        """
    )
    suspend fun get(profileId: String, teamId: String): TeamHubCacheEntity?

    @Upsert
    suspend fun upsert(cache: TeamHubCacheEntity)

    @Query("DELETE FROM TeamHubCacheEntity")
    suspend fun deleteAll()
}
