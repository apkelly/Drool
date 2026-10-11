package com.github.apkelly.drool.home.data

import com.github.apkelly.drool.common.data.repository.SportsDataSource
import com.github.apkelly.drool.home.domain.repository.HomeRepository

class HomeRepositoryImpl(
    private val sportsDataSource: SportsDataSource,
) : HomeRepository {
    override fun observeProfileTeams(
        profileIds: Set<String>,
    ) = sportsDataSource.observeProfileTeams(profileIds)

    override fun observeProfileClubs(
        profileIds: Set<String>,
    ) = sportsDataSource.observeProfileClubs(profileIds)
}
