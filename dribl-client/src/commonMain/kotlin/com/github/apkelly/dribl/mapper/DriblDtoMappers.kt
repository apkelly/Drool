package com.github.apkelly.dribl.mapper

import com.github.apkelly.dribl.api.DriblResponseException
import com.github.apkelly.dribl.dto.AccountDto
import com.github.apkelly.dribl.dto.ClubDto
import com.github.apkelly.dribl.dto.EmergencyContactDto
import com.github.apkelly.dribl.dto.FixtureDto
import com.github.apkelly.dribl.dto.LadderEntryDto
import com.github.apkelly.dribl.dto.LadderEntryResourceDto
import com.github.apkelly.dribl.dto.MatchAttributesDto
import com.github.apkelly.dribl.dto.MatchResourceDto
import com.github.apkelly.dribl.dto.MatchRoleAttributesDto
import com.github.apkelly.dribl.dto.MatchRoleResourceDto
import com.github.apkelly.dribl.dto.PersonDto
import com.github.apkelly.dribl.dto.PersonResourceDto
import com.github.apkelly.dribl.dto.ProfileResponse
import com.github.apkelly.dribl.dto.RefereeAvailabilityResourceDto
import com.github.apkelly.dribl.dto.ScheduleAllocationResourceDto
import com.github.apkelly.dribl.dto.TeamDto
import com.github.apkelly.dribl.model.Account
import com.github.apkelly.dribl.model.AvailabilityPeriod
import com.github.apkelly.dribl.model.Club
import com.github.apkelly.dribl.model.EmergencyContact
import com.github.apkelly.dribl.model.Fixture
import com.github.apkelly.dribl.model.FixtureRole
import com.github.apkelly.dribl.model.FixtureStatus
import com.github.apkelly.dribl.model.LadderEntry
import com.github.apkelly.dribl.model.RefereeAvailability
import com.github.apkelly.dribl.model.RelatedUser
import com.github.apkelly.dribl.model.Team
import com.github.apkelly.dribl.model.normalizedTeamName
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.time.Instant

internal fun ClubDto.toDomain(
    index: Int,
): Club? {
    val resolvedName = name ?: clubName ?: return null
    return Club(
        id = (clubId ?: id).textValue ?: "club-$index-$resolvedName",
        name = resolvedName,
        shortName = shortName ?: code,
        logoUrl = logoUrl ?: image,
        primaryColor = primaryColor ?: color,
        secondaryColor = secondaryColor ?: accent,
    )
}

internal fun TeamDto.toDomain(
    index: Int,
): Team? {
    val resolvedName = name ?: parsedName ?: teamName ?: return null
    val resolvedCompetitionName = competitionName ?: teamCompetitionName
    return Team(
        id = (teamId ?: id).textValue ?: "team-$index-$resolvedName",
        clubId = clubId.textValue,
        name = normalizedTeamName(resolvedName, resolvedCompetitionName),
        shortName = shortName,
        logoUrl = logoUrl ?: image,
        ageGroup = ageGroup,
        competitionName = resolvedCompetitionName,
        active = active,
        primaryColor = primaryColor ?: color,
        secondaryColor = secondaryColor ?: accent,
    )
}

internal fun relatedUserFromResource(
    index: Int,
    resource: PersonResourceDto,
    isLinked: Boolean = false,
): RelatedUser = relatedUserFromPerson(
    index = index,
    person = resource.attributes,
    relationshipId = resource.id.textValue,
    linkId = resource.id.textValue.takeIf { isLinked },
    isLinked = isLinked,
)

internal fun ProfileResponse.profile(): PersonDto? = data?.account ?: data?.profile ?: data?.user ?: profile

internal fun PersonDto.identifier(): String? = (accountId ?: userId ?: id).textValue

internal fun PersonDto.displayName(
    fallback: String,
): String = firstName ?: displayName ?: fullName ?: name ?: email ?: fallback

private fun PersonDto.email(): String? = email ?: emailAddress ?: contactEmail ?: value?.takeIf { it.contains('@') }

private fun PersonDto.identifier(
    index: Int,
    relationshipId: String?,
    userId: String?,
    email: String?,
): String = userId
    ?: relationshipId
    ?: accountId.textValue
    ?: id.textValue
    ?: "related-$index-${email ?: firstName.orEmpty()}"

private fun PersonDto.displayName(
    email: String?,
    fallback: String,
): String = listOfNotNull(firstName, lastName)
    .joinToString(" ")
    .takeIf(String::isNotBlank)
    ?: email
    ?: fallback

internal fun PersonDto.avatar(): String? = avatarUrl ?: profileImage ?: image ?: source

private fun PersonDto.phoneNumber(): String? = phone ?: mobile ?: phoneNumber ?: mobileNumber

internal fun relatedUserFromPerson(
    index: Int,
    person: PersonDto,
    relationshipId: String? = null,
    linkId: String? = null,
    isLinked: Boolean = false,
): RelatedUser {
    val email = person.email()
    val userId = person.userId.textValue
    val id = person.identifier(index, relationshipId, userId, email)
    return RelatedUser(
        id = id,
        displayName = person.displayName(email, id),
        email = email,
        avatarUrl = person.avatar(),
        subjectUserId = userId ?: id,
        linkId = linkId,
        isLinked = isLinked,
        dateOfBirth = person.dateOfBirth ?: person.alternateDateOfBirth,
        gender = person.gender,
        phoneNumber = person.phoneNumber(),
        firstName = person.firstName,
        address = person.formattedAddress(),
        emergencyContacts = person.emergencyContacts
            .mapIndexedNotNull(::emergencyContactFromDto),
        isGuardian = person.isGuardian?.let { it != 0 },
    )
}

internal fun accountFromDto(
    index: Int,
    account: AccountDto,
): Account {
    val id = (account.accountId ?: account.id).textValue ?: "account-$index"
    val attributes = account.attributes
    return Account(
        id = id,
        name = attributes?.name
            ?: attributes?.value
            ?: attributes?.email
            ?: account.accountName
            ?: account.name
            ?: account.value
            ?: id,
        subtitle = attributes?.type
            ?: attributes?.role
            ?: attributes?.organisationName
            ?: account.role
            ?: account.type,
        logoUrl = account.logoUrl ?: account.image,
    )
}

private fun PersonDto.formattedAddress(): String? {
    val value = address
    return listOfNotNull(
        addressLine1 ?: value?.line1,
        addressLine2 ?: value?.line2,
        city ?: value?.city ?: value?.suburb,
        state ?: value?.state,
        postcode ?: value?.postcode,
    ).distinct().joinToString(", ").takeIf(String::isNotBlank)
}

internal fun emergencyContactFromDto(
    index: Int,
    contact: EmergencyContactDto,
): EmergencyContact? {
    val phoneNumber = contact.phoneNumber1
        ?: contact.phone
        ?: contact.mobile
        ?: contact.phoneNumber2
    val email = contact.email ?: contact.contactEmail
    if (phoneNumber == null && email == null) return null
    val name = contact.name
        ?: contact.fullName
        ?: contact.contactName
        ?: "Contact ${index + 1}"
    return EmergencyContact(name, phoneNumber, email)
}

internal fun ladderEntryFromDto(
    index: Int,
    resource: LadderEntryResourceDto,
    competitionName: String?,
): LadderEntry? {
    val entry = resource.fields()
    val teamName = entry.teamName ?: return null
    return LadderEntry(
        position = entry.position ?: index + 1,
        teamId = entry.teamIdentifier,
        teamName = normalizedTeamName(
            name = teamName,
            competitionName = entry.leagueName ?: competitionName,
            allowEmbeddedLeaguePrefix = true,
        ),
        logoUrl = entry.clubLogo ?: entry.image,
        played = entry.played,
        won = entry.won,
        drawn = entry.drawn,
        lost = entry.lost,
        goalsFor = entry.goalsFor,
        goalsAgainst = entry.goalsAgainst,
        goalDifference = entry.goalDifference,
        points = entry.points,
    )
}

internal fun LadderEntryResourceDto.fields(): LadderEntryDto = attributes ?: LadderEntryDto(
    position = position,
    teamHashId = teamId,
    teamName = teamName,
)

internal val LadderEntryDto.teamIdentifier: String?
    get() = (teamId ?: teamHashId).textValue

private fun FixtureDto.resolvedHomeName(): String? = homeTeamName ?: homeTeam?.name

private fun FixtureDto.resolvedAwayName(): String? = awayTeamName ?: awayTeam?.name

private fun FixtureDto.resolvedHomeTeamId(): String? =
    (homeTeamId ?: homeTeamHashId ?: homeTeam?.teamId ?: homeTeam?.id).textValue

private fun FixtureDto.resolvedAwayTeamId(): String? =
    (awayTeamId ?: awayTeamHashId ?: awayTeam?.teamId ?: awayTeam?.id).textValue

private fun FixtureDto.resolvedIdentifier(
    index: Int,
    homeName: String,
    awayName: String,
): String = (id ?: matchId).textValue
    ?: "fixture-$index-$homeName-$awayName"

internal fun FixtureDto.toDomain(
    index: Int,
    competitionName: String? = null,
    venueName: String? = null,
    userTeamId: String? = null,
    status: String? = null,
): Fixture? {
    val resolvedHomeName = resolvedHomeName() ?: return null
    val resolvedAwayName = resolvedAwayName() ?: return null
    val resolvedCompetitionName = this.competitionName ?: leagueName ?: competitionName
    return Fixture(
        id = resolvedIdentifier(index, resolvedHomeName, resolvedAwayName),
        kickoffEpochMillis = (kickoffAt ?: date).epochMillis ?: 0L,
        homeTeamId = resolvedHomeTeamId(),
        homeTeamName = normalizedTeamName(
            name = resolvedHomeName,
            competitionName = resolvedCompetitionName,
            allowEmbeddedLeaguePrefix = this.competitionName == null,
        ),
        awayTeamId = resolvedAwayTeamId(),
        awayTeamName = normalizedTeamName(
            name = resolvedAwayName,
            competitionName = resolvedCompetitionName,
            allowEmbeddedLeaguePrefix = this.competitionName == null,
        ),
        competitionName = resolvedCompetitionName,
        venueName = this.venueName ?: groundName ?: venueName,
        userTeamId = this.userTeamId.textValue ?: userTeamId,
        status = (this.status ?: status).toFixtureStatus(),
        homeScore = homeScore,
        awayScore = awayScore,
        homeTeamLogoUrl = homeTeamLogo ?: homeClubImage,
        awayTeamLogoUrl = awayTeamLogo ?: awayClubImage,
        leagueName = leagueName,
    )
}

internal fun MatchResourceDto.toDomain(): Fixture = attributes.toDomain(id)

private fun MatchAttributesDto.toDomain(
    resourceId: JsonPrimitive,
): Fixture {
    val resolvedCompetitionName = competitionName ?: leagueName
    return Fixture(
        id = resourceId.textValue
            ?: throw DriblResponseException("Match response contained a blank id"),
        kickoffEpochMillis = date.epochMillis
            ?: throw DriblResponseException("Match response contained an invalid date"),
        homeTeamId = homeTeamId.textValue,
        homeTeamName = normalizedTeamName(
            name = homeTeamName,
            competitionName = resolvedCompetitionName,
            allowEmbeddedLeaguePrefix = competitionName == null,
        ),
        awayTeamId = awayTeamId.textValue,
        awayTeamName = normalizedTeamName(
            name = awayTeamName,
            competitionName = resolvedCompetitionName,
            allowEmbeddedLeaguePrefix = competitionName == null,
        ),
        competitionName = resolvedCompetitionName,
        venueName = fieldName,
        userTeamId = null,
        status = status.toFixtureStatus(),
        homeScore = homeTeamScore,
        awayScore = awayTeamScore,
        homeTeamLogoUrl = homeTeamLogo,
        awayTeamLogoUrl = awayTeamLogo,
        roundLabel = roundLabel?.takeIf(String::isNotBlank)
            ?: roundNumber.textValue,
        venueAddress = address,
        latitude = latitude?.doubleOrNull,
        longitude = longitude?.doubleOrNull,
        role = roles.fixtureRole(),
        leagueName = leagueName,
        leagueId = leagueId.textValue,
        seasonId = seasonId.textValue,
        tenantId = tenantId.textValue,
        matchSheetId = matchSheetId.textValue,
    )
}

private val MatchRoleAttributesDto.isReferee: Boolean
    get() = listOfNotNull(name, slug).any {
        it.contains("referee", ignoreCase = true)
    }

private val MatchRoleAttributesDto.isCoach: Boolean
    get() = listOfNotNull(name, slug).any {
        it.contains("coach", ignoreCase = true)
    }

private fun List<MatchRoleResourceDto>.fixtureRole(): FixtureRole = when {
    any { it.attributes.isReferee } -> FixtureRole.Referee
    any { it.attributes.isCoach } -> FixtureRole.Coach
    else -> FixtureRole.Player
}

internal fun ScheduleAllocationResourceDto.toDomain(): Fixture? {
    val matchId = attributes.eventId.textValue
        ?: attributes.matchId.textValue
        ?: id.textValue
        ?: return null
    val kickoff = (attributes.date ?: attributes.startDate).epochMillis ?: return null
    val homeName = attributes.homeTeamName ?: attributes.homeClubName ?: return null
    val awayName = attributes.awayTeamName ?: attributes.awayClubName ?: return null
    val competition = attributes.competitionName ?: attributes.leagueName
    return Fixture(
        id = matchId,
        kickoffEpochMillis = kickoff,
        homeTeamId = attributes.homeTeamId.textValue,
        homeTeamName = normalizedTeamName(homeName, competition),
        awayTeamId = attributes.awayTeamId.textValue,
        awayTeamName = normalizedTeamName(awayName, competition),
        competitionName = competition,
        venueName = listOfNotNull(attributes.ground, attributes.field)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" - ")
            .takeIf(String::isNotBlank),
        userTeamId = null,
        status = attributes.status.toFixtureStatus().takeUnless {
            it == FixtureStatus.Unknown
        } ?: FixtureStatus.Scheduled,
        homeTeamLogoUrl = attributes.homeClubImage,
        awayTeamLogoUrl = attributes.awayClubImage,
        role = FixtureRole.Referee,
        refereeRole = attributes.refereeRole?.takeIf(String::isNotBlank),
        refereeAllocationId = id.textValue,
        refereeStatus = attributes.refereeStatus?.takeIf(String::isNotBlank),
        leagueName = attributes.leagueName,
    )
}

internal fun RefereeAvailabilityResourceDto.toDomain(): RefereeAvailability? {
    val startEpochMillis = attributes.start.epochMillis ?: return null
    val endEpochMillis = attributes.end.epochMillis ?: return null
    val club = attributes.clubs.firstOrNull()
    return RefereeAvailability(
        id = id.textValue ?: return null,
        startEpochMillis = startEpochMillis,
        endEpochMillis = endEpochMillis,
        clubId = attributes.clubId.textValue ?: club?.id.textValue,
        clubName = club?.name ?: club?.clubName,
        period = AvailabilityPeriod.entries.firstOrNull {
            it.name.equals(attributes.templatePeriod, ignoreCase = true)
        } ?: AvailabilityPeriod.SingleDay,
        templateEndEpochMillis = attributes.templateEnd.epochMillis,
    )
}

private fun String?.toFixtureStatus(): FixtureStatus = when (orEmpty().lowercase()) {
    "pending" -> FixtureStatus.Pending
    "scheduled", "upcoming", "fixture" -> FixtureStatus.Scheduled
    "live", "in_progress" -> FixtureStatus.Live
    "completed", "complete", "final" -> FixtureStatus.Completed
    "postponed" -> FixtureStatus.Postponed
    "washout" -> FixtureStatus.Washout
    "cancelled", "canceled" -> FixtureStatus.Cancelled
    else -> FixtureStatus.Unknown
}

private val JsonPrimitive?.textValue: String?
    get() = this?.contentOrNull?.takeUnless { it.isBlank() || it == "null" }

private val JsonPrimitive?.epochMillis: Long?
    get() = this?.longOrNull?.let { raw ->
        if (raw < 100_000_000_000L) raw * 1_000L else raw
    } ?: this?.contentOrNull?.let { value ->
        try {
            Instant.parse(value).toEpochMilliseconds()
        } catch (_: IllegalArgumentException) {
            null
        }
    }
