package com.github.apkelly.drool.common.data.mapper

import com.github.apkelly.drool.common.domain.model.Account
import com.github.apkelly.drool.common.domain.model.AuthenticationIdentity
import com.github.apkelly.drool.common.domain.model.AuthenticationResult
import com.github.apkelly.drool.common.domain.model.AvailabilityPeriod
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.EmergencyContact
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import com.github.apkelly.drool.common.domain.model.LadderEntry
import com.github.apkelly.drool.common.domain.model.LinkMemberRequestResult
import com.github.apkelly.drool.common.domain.model.MatchSheet
import com.github.apkelly.drool.common.domain.model.MatchSheetMember
import com.github.apkelly.drool.common.domain.model.MatchSheetOfficial
import com.github.apkelly.drool.common.domain.model.MatchSheetTeam
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.RefereeAvailability
import com.github.apkelly.drool.common.domain.model.RefereeClub
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.Team
import com.github.apkelly.drool.common.domain.model.TeamAssociation
import com.github.apkelly.drool.common.domain.model.TeamHub
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import com.github.apkelly.drool.common.domain.model.TeamSheetStatus
import com.github.apkelly.dribl.model.Account as DriblAccount
import com.github.apkelly.dribl.model.AuthenticationIdentity as DriblAuthenticationIdentity
import com.github.apkelly.dribl.model.AuthenticationResult as DriblAuthenticationResult
import com.github.apkelly.dribl.model.Club as DriblClub
import com.github.apkelly.dribl.model.EmergencyContact as DriblEmergencyContact
import com.github.apkelly.dribl.model.Fixture as DriblFixture
import com.github.apkelly.dribl.model.LadderEntry as DriblLadderEntry
import com.github.apkelly.dribl.model.LinkMemberRequestResult as DriblLinkMemberRequestResult
import com.github.apkelly.dribl.model.MatchSheet as DriblMatchSheet
import com.github.apkelly.dribl.model.MatchSheetMember as DriblMatchSheetMember
import com.github.apkelly.dribl.model.MatchSheetOfficial as DriblMatchSheetOfficial
import com.github.apkelly.dribl.model.MatchSheetTeam as DriblMatchSheetTeam
import com.github.apkelly.dribl.model.Profile as DriblProfile
import com.github.apkelly.dribl.model.RefereeAvailability as DriblRefereeAvailability
import com.github.apkelly.dribl.model.RefereeClub as DriblRefereeClub
import com.github.apkelly.dribl.model.RelatedUser as DriblRelatedUser
import com.github.apkelly.dribl.model.Team as DriblTeam
import com.github.apkelly.dribl.model.TeamAssociation as DriblTeamAssociation
import com.github.apkelly.dribl.model.TeamHub as DriblTeamHub

internal fun DriblAuthenticationResult.toDomain() = AuthenticationResult(
    bearerToken = bearerToken,
    identity = identity?.toDomain(),
    relatedUsers = relatedUsers.map(DriblRelatedUser::toDomain),
    accounts = accounts.map(DriblAccount::toDomain),
)

private fun DriblAuthenticationIdentity.toDomain() = AuthenticationIdentity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    avatarUrl = avatarUrl,
)

internal fun DriblProfile.toDomain() = Profile(
    accountId = accountId,
    displayName = displayName,
    email = email,
    avatarUrl = avatarUrl,
    relatedUsers = relatedUsers.map(DriblRelatedUser::toDomain),
    accounts = accounts.map(DriblAccount::toDomain),
)

internal fun DriblRelatedUser.toDomain() = RelatedUser(
    id = id,
    displayName = displayName,
    email = email,
    avatarUrl = avatarUrl,
    subjectUserId = subjectUserId,
    linkId = linkId,
    isLinked = isLinked,
    gender = gender,
    dateOfBirth = dateOfBirth,
    phoneNumber = phoneNumber,
    address = address,
    emergencyContacts = emergencyContacts.map(DriblEmergencyContact::toDomain),
    firstName = firstName,
    isGuardian = isGuardian,
)

internal fun DriblEmergencyContact.toDomain() = EmergencyContact(
    name = name,
    phoneNumber = phoneNumber,
    email = email,
)

internal fun DriblAccount.toDomain() = Account(id, name, subtitle, logoUrl)

internal fun DriblClub.toDomain() = Club(
    id = id,
    name = name,
    shortName = shortName,
    logoUrl = logoUrl,
    primaryColor = primaryColor,
    secondaryColor = secondaryColor,
)

internal fun DriblTeam.toDomain() = Team(
    id = id,
    clubId = clubId,
    name = name,
    shortName = shortName,
    logoUrl = logoUrl,
    ageGroup = ageGroup,
    competitionName = competitionName,
    active = active,
    primaryColor = primaryColor,
    secondaryColor = secondaryColor,
)

internal fun DriblTeamAssociation.toDomain() = TeamAssociation(
    team = team.toDomain(),
    relationship = TeamRelationship.valueOf(relationship.name),
)

internal fun DriblFixture.toDomain() = Fixture(
    id = id,
    profileId = profileId,
    kickoffEpochMillis = kickoffEpochMillis,
    homeTeamId = homeTeamId,
    homeTeamName = homeTeamName,
    awayTeamId = awayTeamId,
    awayTeamName = awayTeamName,
    competitionName = competitionName,
    venueName = venueName,
    userTeamId = userTeamId,
    status = FixtureStatus.valueOf(status.name),
    homeScore = homeScore,
    awayScore = awayScore,
    homeTeamLogoUrl = homeTeamLogoUrl,
    awayTeamLogoUrl = awayTeamLogoUrl,
    roundLabel = roundLabel,
    venueAddress = venueAddress,
    latitude = latitude,
    longitude = longitude,
    role = FixtureRole.valueOf(role.name),
    refereeRole = refereeRole,
    refereeAllocationId = refereeAllocationId,
    refereeStatus = refereeStatus,
    leagueName = leagueName,
    leagueId = leagueId,
    seasonId = seasonId,
    tenantId = tenantId,
    matchSheetId = matchSheetId,
)

internal fun Fixture.toDribl() = DriblFixture(
    id = id,
    profileId = profileId,
    kickoffEpochMillis = kickoffEpochMillis,
    homeTeamId = homeTeamId,
    homeTeamName = homeTeamName,
    awayTeamId = awayTeamId,
    awayTeamName = awayTeamName,
    competitionName = competitionName,
    venueName = venueName,
    userTeamId = userTeamId,
    status = com.github.apkelly.dribl.model.FixtureStatus.valueOf(status.name),
    homeScore = homeScore,
    awayScore = awayScore,
    homeTeamLogoUrl = homeTeamLogoUrl,
    awayTeamLogoUrl = awayTeamLogoUrl,
    roundLabel = roundLabel,
    venueAddress = venueAddress,
    latitude = latitude,
    longitude = longitude,
    role = com.github.apkelly.dribl.model.FixtureRole.valueOf(role.name),
    refereeRole = refereeRole,
    refereeAllocationId = refereeAllocationId,
    refereeStatus = refereeStatus,
    leagueName = leagueName,
    leagueId = leagueId,
    seasonId = seasonId,
    tenantId = tenantId,
    matchSheetId = matchSheetId,
)

internal fun DriblTeamHub.toDomain() = TeamHub(
    teamId = teamId,
    matches = matches.map(DriblFixture::toDomain),
    results = results.map(DriblFixture::toDomain),
    ladderName = ladderName,
    ladder = ladder.map(DriblLadderEntry::toDomain),
)

private fun DriblLadderEntry.toDomain() = LadderEntry(
    position,
    teamId,
    teamName,
    logoUrl,
    played,
    won,
    drawn,
    lost,
    goalsFor,
    goalsAgainst,
    goalDifference,
    points,
)

internal fun DriblMatchSheet.toDomain() = MatchSheet(
    id = id,
    status = status,
    homeTeam = homeTeam.toDomain(),
    awayTeam = awayTeam.toDomain(),
    members = members.map(DriblMatchSheetMember::toDomain),
    referees = referees.map(DriblMatchSheetOfficial::toDomain),
    officials = officials.map(DriblMatchSheetOfficial::toDomain),
)

private fun DriblMatchSheetTeam.toDomain() = MatchSheetTeam(
    id,
    name,
    TeamSheetStatus.valueOf(status.name),
)

internal fun DriblMatchSheetMember.toDomain() = MatchSheetMember(
    id,
    userId,
    teamId,
    firstName,
    lastName,
    imageUrl,
    roleName,
    jerseyNumber,
    available,
)

private fun DriblMatchSheetOfficial.toDomain() = MatchSheetOfficial(id, displayName, roleName)

internal fun DriblRefereeClub.toDomain() = RefereeClub(id, name, logoUrl, timeZone)

internal fun DriblRefereeAvailability.toDomain() = RefereeAvailability(
    id,
    startEpochMillis,
    endEpochMillis,
    clubId,
    clubName,
    AvailabilityPeriod.valueOf(period.name),
    templateEndEpochMillis,
)

internal fun DriblLinkMemberRequestResult.toDomain() = LinkMemberRequestResult(
    relationshipId,
    isApproved,
)
