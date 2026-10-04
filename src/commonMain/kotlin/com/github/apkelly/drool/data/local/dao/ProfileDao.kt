package com.github.apkelly.drool.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.github.apkelly.drool.data.local.entity.ProfileEntity
import com.github.apkelly.drool.data.local.entity.RelatedUserEntity
import com.github.apkelly.drool.data.local.entity.AccountEntity

@Dao
interface ProfileDao {
    @Query("SELECT * FROM ProfileEntity WHERE active = 1 LIMIT 1")
    fun observeActive(): Flow<ProfileEntity?>

    @Query("SELECT * FROM ProfileEntity WHERE active = 1 LIMIT 1")
    suspend fun getActive(): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("UPDATE ProfileEntity SET active = 0")
    suspend fun deactivateAll()

    @Query("DELETE FROM ProfileEntity WHERE accountId = :accountId")
    suspend fun delete(accountId: String)

    @Query("DELETE FROM ProfileEntity")
    suspend fun deleteAllProfiles()

    @Query("SELECT * FROM RelatedUserEntity WHERE ownerAccountId = :accountId ORDER BY displayName")
    fun observeRelatedUsers(accountId: String): Flow<List<RelatedUserEntity>>

    @Query("SELECT * FROM RelatedUserEntity WHERE ownerAccountId = :accountId ORDER BY displayName")
    suspend fun getRelatedUsers(accountId: String): List<RelatedUserEntity>

    @Query(
        "SELECT * FROM RelatedUserEntity " +
            "WHERE ownerAccountId = :accountId AND id = :id LIMIT 1"
    )
    suspend fun getRelatedUser(accountId: String, id: String): RelatedUserEntity?

    @Upsert
    suspend fun upsertRelatedUsers(users: List<RelatedUserEntity>)

    @Query("DELETE FROM RelatedUserEntity WHERE ownerAccountId = :accountId")
    suspend fun deleteRelatedUsers(accountId: String)

    @Query("DELETE FROM RelatedUserEntity")
    suspend fun deleteAllRelatedUsers()

    @Query("SELECT * FROM AccountEntity WHERE ownerAccountId = :accountId ORDER BY name")
    fun observeAccounts(accountId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM AccountEntity WHERE ownerAccountId = :accountId ORDER BY name")
    suspend fun getAccounts(accountId: String): List<AccountEntity>

    @Upsert
    suspend fun upsertAccounts(accounts: List<AccountEntity>)

    @Query("DELETE FROM AccountEntity WHERE ownerAccountId = :accountId")
    suspend fun deleteAccounts(accountId: String)

    @Query("DELETE FROM AccountEntity")
    suspend fun deleteAllAccounts()
}
