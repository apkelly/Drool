package com.github.apkelly.drool.domain.model

data class Player(
    val id: String,
    val teamId: String,
    val displayName: String,
    val shirtNumber: Int?,
    val role: PlayerRole,
)

enum class PlayerRole {
    Player,
    Captain,
    Goalkeeper,
    Coach,
    Referee,
    Unknown,
}
