package com.github.apkelly.drool.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TeamHub(
    val teamId: String,
    val matches: List<Fixture>,
    val results: List<Fixture>,
    val ladderName: String?,
    val ladder: List<LadderEntry>,
)

@Serializable
data class LadderEntry(
    val position: Int?,
    val teamId: String?,
    val teamName: String,
    val logoUrl: String?,
    val played: Int?,
    val won: Int?,
    val drawn: Int?,
    val lost: Int?,
    val goalsFor: Int?,
    val goalsAgainst: Int?,
    val goalDifference: Int?,
    val points: Int?,
)
