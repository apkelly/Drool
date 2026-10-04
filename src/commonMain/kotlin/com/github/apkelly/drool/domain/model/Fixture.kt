package com.github.apkelly.drool.domain.model

data class Fixture(
    val id: String,
    val profileId: String? = null,
    val kickoffEpochMillis: Long,
    val homeTeamId: String?,
    val homeTeamName: String,
    val awayTeamId: String?,
    val awayTeamName: String,
    val competitionName: String?,
    val venueName: String?,
    val userTeamId: String?,
    val status: FixtureStatus,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val homeTeamLogoUrl: String? = null,
    val awayTeamLogoUrl: String? = null,
    val roundLabel: String? = null,
    val venueAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

enum class FixtureStatus {
    Pending,
    Scheduled,
    Live,
    Completed,
    Postponed,
    Washout,
    Cancelled,
    Unknown,
}
