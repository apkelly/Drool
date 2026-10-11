package com.github.apkelly.drool.schedule.data

import com.github.apkelly.drool.domain.model.FamilyProfile
import com.github.apkelly.drool.data.repository.SportsDataSource
import com.github.apkelly.drool.schedule.domain.repository.ScheduleRepository

class ScheduleRepositoryImpl(
    private val sportsDataSource: SportsDataSource,
) : ScheduleRepository {
    override fun observeFixtures() = sportsDataSource.observeFixtures()

    override fun observeFamilyFixtures(profileIds: Set<String>) =
        sportsDataSource.observeFamilyFixtures(profileIds)

    override suspend fun refreshFixtures(force: Boolean) =
        sportsDataSource.refreshFixtures(force)

    override suspend fun refreshFamilyProfiles(
        profiles: List<FamilyProfile>,
        force: Boolean,
    ) = sportsDataSource.refreshFamilyProfiles(profiles, force)
}
