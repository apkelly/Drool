package com.github.apkelly.drool.common.domain.model

import kotlinx.serialization.Serializable

data class MatchSheet(
    val id: String,
    val status: String?,
    val homeTeam: MatchSheetTeam,
    val awayTeam: MatchSheetTeam,
    val members: List<MatchSheetMember>,
    val referees: List<MatchSheetOfficial>,
    val officials: List<MatchSheetOfficial>,
    val score: MatchSheetScore? = null,
    val goals: List<MatchSheetGoal> = emptyList(),
)

data class MatchSheetTeam(
    val id: String,
    val name: String,
    val status: TeamSheetStatus,
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
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
    val userId: String? = null,
    val checkedIn: Boolean = false,
)

@Serializable
enum class MatchHalf(
    val apiValue: String,
) {
    FirstHalf("ft-first-half"),
    SecondHalf("ft-second-half"),
}

data class MatchSheetScore(
    val id: String? = null,
    val homeHalfTime: Int? = null,
    val awayHalfTime: Int? = null,
    val homeFullTime: Int? = null,
    val awayFullTime: Int? = null,
)

data class MatchSheetGoal(
    val id: String,
    val teamId: String? = null,
    val matchSheetMemberId: String? = null,
    val memberName: String? = null,
    val jerseyNumber: String? = null,
    val half: MatchHalf = MatchHalf.FirstHalf,
    val minute: Int? = null,
    val ownGoal: Boolean = false,
    val penaltyKick: Boolean = false,
)

data class MatchSheetGoalDraft(
    val matchSheetMemberId: String,
    val half: MatchHalf,
    val minute: Int? = null,
    val ownGoal: Boolean = false,
    val penaltyKick: Boolean = false,
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
