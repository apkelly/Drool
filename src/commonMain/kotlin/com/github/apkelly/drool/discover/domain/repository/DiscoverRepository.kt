package com.github.apkelly.drool.discover.domain.repository

import com.github.apkelly.drool.common.domain.model.CachedData
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.domain.model.Team
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import kotlinx.coroutines.flow.Flow

interface DiscoverRepository {
    fun observeClubs(): Flow<CachedData<List<Club>>>
    fun observeTeams(
        clubId: String?,
    ): Flow<CachedData<List<Team>>>
    fun observeTeamRelationships(): Flow<Map<String, TeamRelationship>>
    suspend fun refreshClubs(
        force: Boolean = false,
    ): RefreshResult
    suspend fun refreshTeams(
        force: Boolean = false,
        clubId: String? = null,
    ): RefreshResult
    suspend fun setFollowing(
        teamId: String,
        following: Boolean,
    )
}
