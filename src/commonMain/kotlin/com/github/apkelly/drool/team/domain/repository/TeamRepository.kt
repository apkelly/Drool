package com.github.apkelly.drool.team.domain.repository

import com.github.apkelly.drool.domain.model.CachedData
import com.github.apkelly.drool.domain.model.TeamHub

interface TeamRepository {
    suspend fun loadTeamHub(
        profileId: String,
        teamId: String,
        force: Boolean = false,
    ): CachedData<TeamHub>
}
