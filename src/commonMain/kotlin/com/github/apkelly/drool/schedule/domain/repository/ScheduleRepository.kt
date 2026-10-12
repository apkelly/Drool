package com.github.apkelly.drool.schedule.domain.repository

import com.github.apkelly.drool.common.domain.model.CachedData
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.common.domain.model.RefereeAllocationDecision
import com.github.apkelly.drool.common.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeFixtures(): Flow<CachedData<List<Fixture>>>
    fun observeLinkedProfileFixtures(
        profileIds: Set<String>,
    ): Flow<List<Fixture>>
    suspend fun refreshFixtures(
        force: Boolean = false,
    ): RefreshResult
    suspend fun refreshLinkedProfiles(
        profiles: List<LinkedProfile>,
        force: Boolean = false,
    ): Map<String, RefreshResult>
    suspend fun updateRefereeAllocation(
        profile: LinkedProfile,
        allocationId: String,
        decision: RefereeAllocationDecision,
    ): RefreshResult
}
