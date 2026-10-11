package com.github.apkelly.drool.home.domain.repository

import kotlinx.coroutines.flow.Flow
import com.github.apkelly.drool.domain.model.FamilyClub
import com.github.apkelly.drool.domain.model.FamilyTeam

interface HomeRepository {
    fun observeFamilyTeams(profileIds: Set<String>): Flow<List<FamilyTeam>>
    fun observeFamilyClubs(profileIds: Set<String>): Flow<List<FamilyClub>>
}
