package com.github.apkelly.drool.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import com.github.apkelly.drool.domain.model.CachedData
import com.github.apkelly.drool.domain.model.FamilyProfile
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.RefreshResult

interface ScheduleRepository {
    fun observeFixtures(): Flow<CachedData<List<Fixture>>>
    fun observeFamilyFixtures(profileIds: Set<String>): Flow<List<Fixture>>
    suspend fun refreshFixtures(force: Boolean = false): RefreshResult
    suspend fun refreshFamilyProfiles(
        profiles: List<FamilyProfile>,
        force: Boolean = false,
    ): Map<String, RefreshResult>
}
