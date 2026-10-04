package com.github.apkelly.drool.domain.repository

import kotlinx.coroutines.flow.Flow
import com.github.apkelly.drool.domain.model.CachedData
import com.github.apkelly.drool.domain.model.Club
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FamilyProfile
import com.github.apkelly.drool.domain.model.FamilyClub
import com.github.apkelly.drool.domain.model.FamilyTeam
import com.github.apkelly.drool.domain.model.RefreshResult
import com.github.apkelly.drool.domain.model.Team
import com.github.apkelly.drool.domain.model.TeamRelationship
import com.github.apkelly.drool.domain.model.TeamHub

interface SportsRepository {
    fun observeClubs(): Flow<CachedData<List<Club>>>
    fun observeTeams(clubId: String?): Flow<CachedData<List<Team>>>
    fun observeFixtures(): Flow<CachedData<List<Fixture>>>
    fun observeFamilyFixtures(profileIds: Set<String>): Flow<List<Fixture>>
    fun observeFamilyTeams(profileIds: Set<String>): Flow<List<FamilyTeam>>
    fun observeFamilyClubs(profileIds: Set<String>): Flow<List<FamilyClub>>
    fun observeTeamRelationships(): Flow<Map<String, TeamRelationship>>

    suspend fun refreshClubs(force: Boolean = false): RefreshResult
    suspend fun refreshTeams(
        force: Boolean = false,
        clubId: String? = null,
    ): RefreshResult
    suspend fun refreshFixtures(force: Boolean = false): RefreshResult
    suspend fun refreshFamilyProfiles(
        profiles: List<FamilyProfile>,
        force: Boolean = false,
    ): Map<String, RefreshResult>
    suspend fun setFollowing(teamId: String, following: Boolean)
    suspend fun loadTeamHub(profileId: String, teamId: String): TeamHub
    suspend fun loadMatchDetails(profileId: String, matchId: String): Fixture
}
