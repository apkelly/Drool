package com.github.apkelly.drool.home.domain.repository

import com.github.apkelly.drool.common.domain.model.ProfileClub
import com.github.apkelly.drool.common.domain.model.ProfileTeam
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeProfileTeams(
        profileIds: Set<String>,
    ): Flow<List<ProfileTeam>>
    fun observeProfileClubs(
        profileIds: Set<String>,
    ): Flow<List<ProfileClub>>
}
