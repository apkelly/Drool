package com.github.apkelly.drool.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.github.apkelly.drool.data.local.entity.CacheMetadataEntity

@Dao
interface CacheMetadataDao {
    @Query("SELECT * FROM CacheMetadataEntity WHERE cacheKey = :cacheKey AND accountScope = :accountScope LIMIT 1")
    fun observe(cacheKey: String, accountScope: String): Flow<CacheMetadataEntity?>

    @Query("SELECT * FROM CacheMetadataEntity WHERE cacheKey = :cacheKey AND accountScope = :accountScope LIMIT 1")
    suspend fun get(cacheKey: String, accountScope: String): CacheMetadataEntity?

    @Upsert
    suspend fun upsert(metadata: CacheMetadataEntity)

    @Query("DELETE FROM CacheMetadataEntity WHERE accountScope = :accountScope")
    suspend fun deleteForAccount(accountScope: String)

    @Query("DELETE FROM CacheMetadataEntity")
    suspend fun deleteAll()
}
