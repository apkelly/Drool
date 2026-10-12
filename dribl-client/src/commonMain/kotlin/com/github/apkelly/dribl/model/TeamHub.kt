package com.github.apkelly.dribl.model

data class TeamHub(
    val teamId: String,
    val matches: List<Fixture>,
    val results: List<Fixture>,
    val ladderName: String?,
    val ladder: List<LadderEntry>,
)
