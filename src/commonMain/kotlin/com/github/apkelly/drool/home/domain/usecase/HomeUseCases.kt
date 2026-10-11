package com.github.apkelly.drool.home.domain.usecase

import com.github.apkelly.drool.home.domain.repository.HomeRepository

class ObserveFamilyTeamsUseCase(
    private val repository: HomeRepository,
) {
    operator fun invoke(
        profileIds: Set<String>,
    ) = repository.observeFamilyTeams(profileIds)
}

class ObserveFamilyClubsUseCase(
    private val repository: HomeRepository,
) {
    operator fun invoke(
        profileIds: Set<String>,
    ) = repository.observeFamilyClubs(profileIds)
}
