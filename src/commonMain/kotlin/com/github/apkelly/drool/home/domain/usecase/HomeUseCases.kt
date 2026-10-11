package com.github.apkelly.drool.home.domain.usecase

import com.github.apkelly.drool.home.domain.repository.HomeRepository

class ObserveProfileTeamsUseCase(
    private val repository: HomeRepository,
) {
    operator fun invoke(
        profileIds: Set<String>,
    ) = repository.observeProfileTeams(profileIds)
}

class ObserveProfileClubsUseCase(
    private val repository: HomeRepository,
) {
    operator fun invoke(
        profileIds: Set<String>,
    ) = repository.observeProfileClubs(profileIds)
}
