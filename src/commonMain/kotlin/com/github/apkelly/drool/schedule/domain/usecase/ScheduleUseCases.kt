package com.github.apkelly.drool.schedule.domain.usecase

import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.schedule.domain.repository.ScheduleRepository

class ObserveFixturesUseCase(
    private val repository: ScheduleRepository,
) {
    operator fun invoke() = repository.observeFixtures()
    operator fun invoke(
        profileIds: Set<String>,
    ) = repository.observeLinkedProfileFixtures(profileIds)
}

class RefreshFixturesUseCase(
    private val repository: ScheduleRepository,
) {
    suspend operator fun invoke(
        force: Boolean = true,
    ) = repository.refreshFixtures(force)
}

class RefreshLinkedProfilesUseCase(
    private val repository: ScheduleRepository,
) {
    suspend operator fun invoke(
        profiles: List<LinkedProfile>,
        force: Boolean = true,
    ) = repository.refreshLinkedProfiles(profiles, force)
}
