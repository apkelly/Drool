package com.github.apkelly.drool.domain.model

data class Team(
    val id: String,
    val clubId: String?,
    val name: String,
    val shortName: String?,
    val logoUrl: String?,
    val ageGroup: String?,
    val competitionName: String?,
    val active: Boolean?,
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
)

data class FamilyTeam(
    val profileId: String,
    val team: Team,
    val relationship: TeamRelationship,
)

data class TeamAssociation(
    val team: Team,
    val relationship: TeamRelationship,
)

enum class TeamRelationship {
    None,
    Following,
    Player,
    Coaching,
}
