package com.github.apkelly.drool.common.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
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
    version = 16,
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

internal val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(
        connection: SQLiteConnection,
    ) {
        connection.execSQL("ALTER TABLE `FixtureEntity` ADD COLUMN `roundLabel` TEXT")
        connection.execSQL("ALTER TABLE `FixtureEntity` ADD COLUMN `venueAddress` TEXT")
        connection.execSQL("ALTER TABLE `FixtureEntity` ADD COLUMN `latitude` REAL")
        connection.execSQL("ALTER TABLE `FixtureEntity` ADD COLUMN `longitude` REAL")
        connection.execSQL("ALTER TABLE `FixtureEntity` ADD COLUMN `leagueId` TEXT")
        connection.execSQL("ALTER TABLE `FixtureEntity` ADD COLUMN `seasonId` TEXT")
        connection.execSQL("ALTER TABLE `FixtureEntity` ADD COLUMN `tenantId` TEXT")
        connection.execSQL("ALTER TABLE `FixtureEntity` ADD COLUMN `matchSheetId` TEXT")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_FixtureEntity_accountId_kickoffEpochMillis` " +
                "ON `FixtureEntity` (`accountId`, `kickoffEpochMillis`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_FixtureEntity_accountId_role_kickoffEpochMillis` " +
                "ON `FixtureEntity` (`accountId`, `role`, `kickoffEpochMillis`)",
        )
    }
}

fun createDroolDatabase(): DroolDatabase = createPlatformDatabaseBuilder()
    .addMigrations(MIGRATION_15_16)
    .fallbackToDestructiveMigration(dropAllTables = true)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()
