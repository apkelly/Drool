package com.github.apkelly.drool.schedule.data

import com.github.apkelly.drool.common.data.repository.SportsDataSource
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.schedule.domain.repository.ScheduleRepository

class ScheduleRepositoryImpl(
    private val sportsDataSource: SportsDataSource,
) : ScheduleRepository {
    override fun observeFixtures() = sportsDataSource.observeFixtures()

    override fun observeLinkedProfileFixtures(
        profileIds: Set<String>,
    ) = sportsDataSource.observeLinkedProfileFixtures(profileIds)

    override suspend fun refreshFixtures(
        force: Boolean,
    ) = sportsDataSource.refreshFixtures(force)

    override suspend fun refreshLinkedProfiles(
        profiles: List<LinkedProfile>,
        force: Boolean,
    ) = sportsDataSource.refreshLinkedProfiles(profiles, force)
}
