package com.github.apkelly.drool.common.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.github.apkelly.drool.common.data.local.entity.FixtureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixtureDao {
    @Query("SELECT * FROM FixtureEntity WHERE accountId = :accountId ORDER BY kickoffEpochMillis")
    fun observeForAccount(
        accountId: String,
    ): Flow<List<FixtureEntity>>

    @Query("SELECT * FROM FixtureEntity WHERE accountId IN (:accountIds) ORDER BY kickoffEpochMillis")
    fun observeForAccounts(
        accountIds: List<String>,
    ): Flow<List<FixtureEntity>>

    @Query("SELECT COUNT(*) FROM FixtureEntity WHERE accountId = :accountId")
    suspend fun countForAccount(
        accountId: String,
    ): Int

    @Upsert
    suspend fun upsertAll(
        fixtures: List<FixtureEntity>,
    )

    @Query("DELETE FROM FixtureEntity WHERE accountId = :accountId")
    suspend fun deleteForAccount(
        accountId: String,
    )

    @Query("DELETE FROM FixtureEntity")
    suspend fun deleteAll()
}
