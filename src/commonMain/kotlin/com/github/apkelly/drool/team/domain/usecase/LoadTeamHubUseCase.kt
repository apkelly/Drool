package com.github.apkelly.drool.team.domain.usecase

import com.github.apkelly.drool.team.domain.repository.TeamRepository

class LoadTeamHubUseCase(
    private val repository: TeamRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        teamId: String,
        competitionName: String? = null,
        leagueId: String? = null,
        force: Boolean = false,
    ) = repository.loadTeamHub(profileId, teamId, competitionName, leagueId, force)
}
