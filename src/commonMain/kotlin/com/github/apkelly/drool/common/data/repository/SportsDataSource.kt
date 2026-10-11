package com.github.apkelly.drool.common.data.repository

import com.github.apkelly.drool.common.domain.model.CachedData
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.common.domain.model.ProfileClub
import com.github.apkelly.drool.common.domain.model.ProfileTeam
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.domain.model.Team
import com.github.apkelly.drool.common.domain.model.TeamHub
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import kotlinx.coroutines.flow.Flow

interface SportsDataSource {
    fun observeClubs(): Flow<CachedData<List<Club>>>
    fun observeTeams(
        clubId: String?,
    ): Flow<CachedData<List<Team>>>
    fun observeFixtures(): Flow<CachedData<List<Fixture>>>
    fun observeLinkedProfileFixtures(
        profileIds: Set<String>,
    ): Flow<List<Fixture>>
    fun observeProfileTeams(
        profileIds: Set<String>,
    ): Flow<List<ProfileTeam>>
    fun observeProfileClubs(
        profileIds: Set<String>,
    ): Flow<List<ProfileClub>>
    fun observeTeamRelationships(): Flow<Map<String, TeamRelationship>>

    suspend fun refreshClubs(
        force: Boolean = false,
    ): RefreshResult
    suspend fun refreshTeams(
        force: Boolean = false,
        clubId: String? = null,
    ): RefreshResult
    suspend fun refreshFixtures(
        force: Boolean = false,
    ): RefreshResult
    suspend fun refreshLinkedProfiles(
        profiles: List<LinkedProfile>,
        force: Boolean = false,
    ): Map<String, RefreshResult>
    suspend fun setFollowing(
        teamId: String,
        following: Boolean,
    )
    suspend fun loadTeamHub(
        profileId: String,
        teamId: String,
        force: Boolean = false,
    ): CachedData<TeamHub>
    suspend fun loadMatchDetails(
        profileId: String,
        matchId: String,
    ): Fixture
}
