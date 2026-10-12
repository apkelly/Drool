package com.github.apkelly.drool.common.domain.model

data class MatchSheet(
    val id: String,
    val status: String?,
    val homeTeam: MatchSheetTeam,
    val awayTeam: MatchSheetTeam,
    val members: List<MatchSheetMember>,
    val referees: List<MatchSheetOfficial>,
    val officials: List<MatchSheetOfficial>,
)

data class MatchSheetTeam(
    val id: String,
    val name: String,
    val status: TeamSheetStatus,
)

data class MatchSheetMember(
    val id: String,
    val userId: String?,
    val teamId: String?,
    val firstName: String,
    val lastName: String,
    val imageUrl: String?,
    val roleName: String,
    val jerseyNumber: String?,
    val available: Boolean,
) {
    val displayName: String
        get() = "$firstName $lastName".trim().ifBlank { roleName }
}

data class MatchSheetOfficial(
    val id: String,
    val displayName: String,
    val roleName: String?,
)

enum class TeamSheetStatus {
    Open,
    Submitted,
    Confirmed,
}

enum class TeamSheetAction {
    Submit,
    Unsubmit,
    Confirm,
    Unconfirm,
}
