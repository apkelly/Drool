package com.github.apkelly.drool.common.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.apkelly.drool.common.data.local.dao.CacheMetadataDao
import com.github.apkelly.drool.common.data.local.dao.ClubDao
import com.github.apkelly.drool.common.data.local.dao.FixtureDao
import com.github.apkelly.drool.common.data.local.dao.MatchWeatherCacheDao
import com.github.apkelly.drool.common.data.local.dao.ProfileDao
import com.github.apkelly.drool.common.data.local.dao.RefereeAssignmentSnapshotDao
import com.github.apkelly.drool.common.data.local.dao.TeamDao
import com.github.apkelly.drool.common.data.local.dao.TeamHubCacheDao
import com.github.apkelly.drool.common.data.local.entity.AccountEntity
import com.github.apkelly.drool.common.data.local.entity.CacheMetadataEntity
import com.github.apkelly.drool.common.data.local.entity.ClubEntity
import com.github.apkelly.drool.common.data.local.entity.ClubRelationshipEntity
import com.github.apkelly.drool.common.data.local.entity.FixtureEntity
import com.github.apkelly.drool.common.data.local.entity.MatchWeatherCacheEntity
import com.github.apkelly.drool.common.data.local.entity.ProfileEntity
import com.github.apkelly.drool.common.data.local.entity.RefereeAssignmentSnapshotEntity
import com.github.apkelly.drool.common.data.local.entity.RelatedUserEntity
import com.github.apkelly.drool.common.data.local.entity.TeamEntity
import com.github.apkelly.drool.common.data.local.entity.TeamHubCacheEntity
import com.github.apkelly.drool.common.data.local.entity.TeamRelationshipEntity
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        CacheMetadataEntity::class,
        AccountEntity::class,
        ClubEntity::class,
        ClubRelationshipEntity::class,
        FixtureEntity::class,
        ProfileEntity::class,
        RelatedUserEntity::class,
        RefereeAssignmentSnapshotEntity::class,
        TeamEntity::class,
        TeamRelationshipEntity::class,
        TeamHubCacheEntity::class,
        MatchWeatherCacheEntity::class,
    ],
    version = 15,
    exportSchema = true,
)
@ConstructedBy(DroolDatabaseConstructor::class)
abstract class DroolDatabase : RoomDatabase() {
    abstract fun cacheMetadataDao(): CacheMetadataDao
    abstract fun clubDao(): ClubDao
    abstract fun fixtureDao(): FixtureDao
    abstract fun profileDao(): ProfileDao
    abstract fun refereeAssignmentSnapshotDao(): RefereeAssignmentSnapshotDao
    abstract fun teamDao(): TeamDao
    abstract fun teamHubCacheDao(): TeamHubCacheDao
    abstract fun matchWeatherCacheDao(): MatchWeatherCacheDao
}

@Suppress("KotlinNoActualForExpect")
expect object DroolDatabaseConstructor : RoomDatabaseConstructor<DroolDatabase> {
    override fun initialize(): DroolDatabase
}

expect fun createPlatformDatabaseBuilder(): RoomDatabase.Builder<DroolDatabase>

fun createDroolDatabase(): DroolDatabase = createPlatformDatabaseBuilder()
    .fallbackToDestructiveMigration(dropAllTables = true)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()
