package com.github.apkelly.dribl.mapper

import com.github.apkelly.dribl.api.DriblResponseException
import com.github.apkelly.dribl.dto.MatchSheetGoalResourceDto
import com.github.apkelly.dribl.dto.MatchSheetMemberResourceDto
import com.github.apkelly.dribl.dto.MatchSheetOfficialResourceDto
import com.github.apkelly.dribl.dto.MatchSheetScoreResourceDto
import com.github.apkelly.dribl.model.MatchHalf
import com.github.apkelly.dribl.model.MatchSheetGoal
import com.github.apkelly.dribl.model.MatchSheetMember
import com.github.apkelly.dribl.model.MatchSheetOfficial
import com.github.apkelly.dribl.model.MatchSheetScore
import com.github.apkelly.dribl.model.TeamSheetStatus
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal fun MatchSheetMemberResourceDto.toDomain(): MatchSheetMember {
    val firstName = attributes.firstNameAlias ?: attributes.firstName.orEmpty()
    val lastName = attributes.lastNameAlias ?: attributes.lastName.orEmpty()
    return MatchSheetMember(
        id = id.textValue
            ?: throw DriblResponseException("Matchsheet member response contained a blank id"),
        userId = attributes.userId.textValue,
        teamId = attributes.teamId.textValue,
        firstName = firstName,
        lastName = lastName,
        imageUrl = attributes.imageUrl,
        roleName = attributes.roleName?.takeIf(String::isNotBlank) ?: "Player",
        jerseyNumber = attributes.jerseyNumber.textValue,
        available = attributes.available,
    )
}

internal fun MatchSheetOfficialResourceDto.toDomain(): MatchSheetOfficial {
    val firstName = attributes.userFirstName ?: attributes.firstName
    val lastName = attributes.userLastName ?: attributes.lastName
    val displayName = listOfNotNull(firstName, lastName)
        .joinToString(" ")
        .takeIf(String::isNotBlank)
        ?: attributes.name
        ?: attributes.refereeName?.takeIf(String::isNotBlank)
        ?: "Official"
    return MatchSheetOfficial(
        id = id.textValue
            ?: throw DriblResponseException("Matchsheet official response contained a blank id"),
        displayName = displayName,
        roleName = attributes.refereeRole ?: attributes.roleName ?: attributes.type,
        userId = attributes.userId.textValue,
        checkedIn = attributes.presence.equals("present", ignoreCase = true),
    )
}

internal fun MatchSheetScoreResourceDto.toDomain(): MatchSheetScore = MatchSheetScore(
    id = id.textValue,
    homeHalfTime = attributes.homeHalfTime,
    awayHalfTime = attributes.awayHalfTime,
    homeFullTime = attributes.homeFullTime,
    awayFullTime = attributes.awayFullTime,
)

internal fun MatchSheetGoalResourceDto.toDomain(): MatchSheetGoal = MatchSheetGoal(
    id = id.textValue
        ?: throw DriblResponseException("Matchsheet goal response contained a blank id"),
    teamId = attributes.teamId.textValue,
    matchSheetMemberId = attributes.matchSheetMemberId.textValue,
    memberName = attributes.matchSheetMemberName?.takeIf(String::isNotBlank),
    jerseyNumber = attributes.jerseyNumber.textValue,
    half = MatchHalf.fromApiValue(attributes.gameSection),
    minute = attributes.minute,
    ownGoal = attributes.ownGoal,
    penaltyKick = attributes.penaltyKick,
)

internal fun String?.toTeamSheetStatus(): TeamSheetStatus = when (this?.lowercase()) {
    "submitted" -> TeamSheetStatus.Submitted
    "confirmed" -> TeamSheetStatus.Confirmed
    else -> TeamSheetStatus.Open
}

private val JsonPrimitive?.textValue: String?
    get() = this?.contentOrNull?.takeUnless { it.isBlank() || it == "null" }
