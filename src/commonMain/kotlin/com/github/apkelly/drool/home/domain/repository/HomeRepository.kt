package com.github.apkelly.drool.home.domain.repository

import com.github.apkelly.drool.domain.model.FamilyClub
import com.github.apkelly.drool.domain.model.FamilyTeam
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeFamilyTeams(
        profileIds: Set<String>,
    ): Flow<List<FamilyTeam>>
    fun observeFamilyClubs(
        profileIds: Set<String>,
    ): Flow<List<FamilyClub>>
}
