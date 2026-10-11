package com.github.apkelly.drool.home.data

import com.github.apkelly.drool.common.data.repository.SportsDataSource
import com.github.apkelly.drool.home.domain.repository.HomeRepository

class HomeRepositoryImpl(
    private val sportsDataSource: SportsDataSource,
) : HomeRepository {
    override fun observeFamilyTeams(
        profileIds: Set<String>,
    ) = sportsDataSource.observeFamilyTeams(profileIds)

    override fun observeFamilyClubs(
        profileIds: Set<String>,
    ) = sportsDataSource.observeFamilyClubs(profileIds)
}
