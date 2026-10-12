package com.github.apkelly.drool.common.data.repository

import com.github.apkelly.dribl.api.SportsRemoteDataSource
import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.data.time.TimeProvider
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.common.domain.model.RefereeAllocationDecision
import com.github.apkelly.drool.discover.data.DiscoverRepositoryImpl
import com.github.apkelly.drool.home.data.HomeRepositoryImpl
import com.github.apkelly.drool.match.data.MatchRepositoryImpl
import com.github.apkelly.drool.match.data.weather.MatchWeatherDataSource
import com.github.apkelly.drool.schedule.data.ScheduleRepositoryImpl
import com.github.apkelly.drool.team.data.TeamRepositoryImpl

internal class TestSportsRepositories(
    api: SportsRemoteDataSource,
    database: DroolDatabase,
    store: BearerTokenStore,
    timeProvider: TimeProvider,
) {
    private val discover = DiscoverRepositoryImpl(api, database, store, timeProvider)
    private val home = HomeRepositoryImpl(database)
    private val schedule = ScheduleRepositoryImpl(api, database, store, timeProvider)
    private val team = TeamRepositoryImpl(api, database, store, timeProvider)
    private val match = MatchRepositoryImpl(
        api = api,
        store = store,
        weatherDataSource = object : MatchWeatherDataSource {
            override suspend fun loadMatchWeather(
                fixture: com.github.apkelly.drool.common.domain.model.Fixture,
                force: Boolean,
            ) = null
        },
    )

    fun observeClubs() = discover.observeClubs()
    fun observeTeams(
        clubId: String?,
    ) = discover.observeTeams(clubId)
    fun observeFixtures() = schedule.observeFixtures()
    fun observeLinkedProfileFixtures(
        profileIds: Set<String>,
    ) = schedule.observeLinkedProfileFixtures(profileIds)
    fun observeProfileTeams(
        profileIds: Set<String>,
    ) = home.observeProfileTeams(profileIds)
    fun observeProfileClubs(
        profileIds: Set<String>,
    ) = home.observeProfileClubs(profileIds)
    fun observeTeamRelationships() = discover.observeTeamRelationships()

    suspend fun refreshClubs(
        force: Boolean = false,
    ) = discover.refreshClubs(force)
    suspend fun refreshTeams(
        force: Boolean = false,
        clubId: String? = null,
    ) = discover.refreshTeams(force, clubId)
    suspend fun refreshFixtures(
        force: Boolean = false,
    ) = schedule.refreshFixtures(force)
    suspend fun refreshLinkedProfiles(
        profiles: List<LinkedProfile>,
        force: Boolean = false,
    ) = schedule.refreshLinkedProfiles(profiles, force)
    suspend fun updateRefereeAllocation(
        profile: LinkedProfile,
        allocationId: String,
        decision: RefereeAllocationDecision,
    ) = schedule.updateRefereeAllocation(profile, allocationId, decision)
    suspend fun setFollowing(
        teamId: String,
        following: Boolean,
    ) = discover.setFollowing(teamId, following)

    suspend fun loadTeamHub(
        profileId: String,
        teamId: String,
        competitionName: String? = null,
        leagueId: String? = null,
        force: Boolean = false,
    ) = team.loadTeamHub(
        profileId,
        teamId,
        competitionName,
        leagueId,
        force,
    )

    suspend fun loadMatchDetails(
        profileId: String,
        matchId: String,
    ) = match.loadMatchDetails(profileId, matchId)

    suspend fun loadMatchWeather(
        fixture: Fixture,
    ) = match.loadMatchWeather(fixture)
}
