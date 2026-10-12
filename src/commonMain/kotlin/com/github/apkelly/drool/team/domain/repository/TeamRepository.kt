package com.github.apkelly.drool.team.domain.repository

import com.github.apkelly.drool.common.domain.model.CachedData
import com.github.apkelly.drool.common.domain.model.OperationResult
import com.github.apkelly.drool.common.domain.model.TeamHub

interface TeamRepository {
    suspend fun loadTeamHub(
        profileId: String,
        teamId: String,
        force: Boolean = false,
    ): OperationResult<CachedData<TeamHub>>
}
