package com.github.apkelly.drool.schedule.domain.usecase

import com.github.apkelly.drool.common.domain.model.FamilyProfile
import com.github.apkelly.drool.schedule.domain.repository.ScheduleRepository

class ObserveFixturesUseCase(
    private val repository: ScheduleRepository,
) {
    operator fun invoke() = repository.observeFixtures()
    operator fun invoke(
        profileIds: Set<String>,
    ) = repository.observeFamilyFixtures(profileIds)
}

class RefreshFixturesUseCase(
    private val repository: ScheduleRepository,
) {
    suspend operator fun invoke(
        force: Boolean = true,
    ) = repository.refreshFixtures(force)
}

class RefreshFamilyProfilesUseCase(
    private val repository: ScheduleRepository,
) {
    suspend operator fun invoke(
        profiles: List<FamilyProfile>,
        force: Boolean = true,
    ) = repository.refreshFamilyProfiles(profiles, force)
}
