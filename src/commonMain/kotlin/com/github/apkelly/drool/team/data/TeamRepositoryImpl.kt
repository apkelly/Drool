package com.github.apkelly.drool.team.data

import com.github.apkelly.drool.common.data.repository.SportsDataSource
import com.github.apkelly.drool.team.domain.repository.TeamRepository

class TeamRepositoryImpl(
    private val sportsDataSource: SportsDataSource,
) : TeamRepository {
    override suspend fun loadTeamHub(
        profileId: String,
        teamId: String,
        force: Boolean,
    ) = sportsDataSource.loadTeamHub(profileId, teamId, force)
}
