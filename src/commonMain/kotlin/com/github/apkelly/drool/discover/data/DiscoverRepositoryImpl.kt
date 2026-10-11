package com.github.apkelly.drool.discover.data

import com.github.apkelly.drool.discover.domain.repository.DiscoverRepository
import com.github.apkelly.drool.data.repository.SportsDataSource

class DiscoverRepositoryImpl(
    private val sportsDataSource: SportsDataSource,
) : DiscoverRepository {
    override fun observeClubs() = sportsDataSource.observeClubs()

    override fun observeTeams(clubId: String?) = sportsDataSource.observeTeams(clubId)

    override fun observeTeamRelationships() = sportsDataSource.observeTeamRelationships()

    override suspend fun refreshClubs(force: Boolean) =
        sportsDataSource.refreshClubs(force)

    override suspend fun refreshTeams(force: Boolean, clubId: String?) =
        sportsDataSource.refreshTeams(force, clubId)

    override suspend fun setFollowing(teamId: String, following: Boolean) =
        sportsDataSource.setFollowing(teamId, following)
}
