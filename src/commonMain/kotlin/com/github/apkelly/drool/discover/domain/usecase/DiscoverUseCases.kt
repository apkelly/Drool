package com.github.apkelly.drool.discover.domain.usecase

import com.github.apkelly.drool.discover.domain.repository.DiscoverRepository

class ObserveClubsUseCase(
    private val repository: DiscoverRepository,
) {
    operator fun invoke() = repository.observeClubs()
}

class ObserveTeamsUseCase(
    private val repository: DiscoverRepository,
) {
    operator fun invoke(
        clubId: String? = null,
    ) = repository.observeTeams(clubId)
}

class ObserveTeamRelationshipsUseCase(
    private val repository: DiscoverRepository,
) {
    operator fun invoke() = repository.observeTeamRelationships()
}

class RefreshClubsUseCase(
    private val repository: DiscoverRepository,
) {
    suspend operator fun invoke(
        force: Boolean = true,
    ) = repository.refreshClubs(force)
}

class RefreshTeamsUseCase(
    private val repository: DiscoverRepository,
) {
    suspend operator fun invoke(
        force: Boolean = true,
        clubId: String? = null,
    ) = repository.refreshTeams(force, clubId)
}

class SetTeamFollowingUseCase(
    private val repository: DiscoverRepository,
) {
    suspend operator fun invoke(
        teamId: String,
        following: Boolean,
    ) = repository.setFollowing(teamId, following)
}
