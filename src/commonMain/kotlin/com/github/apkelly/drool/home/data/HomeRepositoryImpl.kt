package com.github.apkelly.drool.home.data

import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.mapper.toDomain
import com.github.apkelly.drool.common.data.repository.relationshipFrom
import com.github.apkelly.drool.common.domain.model.ProfileClub
import com.github.apkelly.drool.common.domain.model.ProfileTeam
import com.github.apkelly.drool.home.domain.repository.HomeRepository
import kotlinx.coroutines.flow.combine

class HomeRepositoryImpl(
    private val database: DroolDatabase,
) : HomeRepository {
    override fun observeProfileTeams(
        profileIds: Set<String>,
    ) = combine(
        database.teamDao().observeForAccounts(profileIds.toList()),
        database.teamDao().observeRelationships(profileIds.toList()),
    ) { teams, relationships ->
        val teamsById = teams.associateBy { it.id }
        relationships.mapNotNull { relationship ->
            teamsById[relationship.teamId]?.let { team ->
                ProfileTeam(
                    profileId = relationship.accountId,
                    team = team.toDomain(),
                    relationship = relationshipFrom(relationship.relationship),
                )
            }
        }
    }

    override fun observeProfileClubs(
        profileIds: Set<String>,
    ) = combine(
        database.clubDao().observeForAccounts(profileIds.toList()),
        database.clubDao().observeRelationships(profileIds.toList()),
    ) { clubs, relationships ->
        val clubsById = clubs.associateBy { it.id }
        relationships.mapNotNull { relationship ->
            clubsById[relationship.clubId]?.let { club ->
                ProfileClub(relationship.accountId, club.toDomain())
            }
        }
    }
}
