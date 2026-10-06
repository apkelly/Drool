package com.github.apkelly.drool.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.github.apkelly.drool.data.local.entity.MatchWeatherCacheEntity

@Dao
interface MatchWeatherCacheDao {
    @Query("SELECT * FROM MatchWeatherCacheEntity WHERE matchId = :matchId LIMIT 1")
    suspend fun get(matchId: String): MatchWeatherCacheEntity?

    @Upsert
    suspend fun upsert(cache: MatchWeatherCacheEntity)

    @Query("DELETE FROM MatchWeatherCacheEntity")
    suspend fun deleteAll()
}
