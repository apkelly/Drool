package com.github.apkelly.drool.common.data.mapper

internal fun com.github.apkelly.drool.common.domain.model.AuthCredentials.toDriblRequest() =
    com.github.apkelly.dribl.model.AuthApiRequest(
        baseUrl = baseUrl,
        path = path,
        username = username,
        password = password,
    )

internal fun com.github.apkelly.dribl.model.AuthenticationResult.toDomain() =
    com.github.apkelly.drool.common.domain.model.AuthenticationResult(
        bearerToken = bearerToken,
        identity = identity?.toDomain(),
        relatedUsers = relatedUsers.map { it.toDomain() },
        accounts = accounts.map { it.toDomain() },
    )

private fun com.github.apkelly.dribl.model.AuthenticationIdentity.toDomain() =
    com.github.apkelly.drool.common.domain.model.AuthenticationIdentity(
        id = id,
        firstName = firstName,
        lastName = lastName,
        email = email,
        avatarUrl = avatarUrl,
    )

internal fun com.github.apkelly.dribl.model.Profile.toDomain() = com.github.apkelly.drool.common.domain.model.Profile(
    accountId = accountId,
    displayName = displayName,
    email = email,
    avatarUrl = avatarUrl,
    relatedUsers = relatedUsers.map { it.toDomain() },
    accounts = accounts.map { it.toDomain() },
)

internal fun com.github.apkelly.dribl.model.RelatedUser.toDomain() =
    com.github.apkelly.drool.common.domain.model.RelatedUser(
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
        emergencyContacts = emergencyContacts.map { it.toDomain() },
        firstName = firstName,
        isGuardian = isGuardian,
    )

internal fun com.github.apkelly.dribl.model.EmergencyContact.toDomain() =
    com.github.apkelly.drool.common.domain.model.EmergencyContact(
        name = name,
        phoneNumber = phoneNumber,
        email = email,
    )

internal fun com.github.apkelly.dribl.model.Account.toDomain() = com.github.apkelly.drool.common.domain.model.Account(
    id = id,
    name = name,
    subtitle = subtitle,
    logoUrl = logoUrl,
)

internal fun com.github.apkelly.dribl.model.Club.toDomain() = com.github.apkelly.drool.common.domain.model.Club(
    id = id,
    name = name,
    shortName = shortName,
    logoUrl = logoUrl,
    primaryColor = primaryColor,
    secondaryColor = secondaryColor,
)

internal fun com.github.apkelly.dribl.model.Team.toDomain() = com.github.apkelly.drool.common.domain.model.Team(
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

internal fun com.github.apkelly.dribl.model.TeamAssociation.toDomain() =
    com.github.apkelly.drool.common.domain.model.TeamAssociation(
        team = team.toDomain(),
        relationship = com.github.apkelly.drool.common.domain.model.TeamRelationship.valueOf(
            relationship.name,
        ),
    )

internal fun com.github.apkelly.dribl.model.Fixture.toDomain() = com.github.apkelly.drool.common.domain.model.Fixture(
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
    status = com.github.apkelly.drool.common.domain.model.FixtureStatus.valueOf(status.name),
    homeScore = homeScore,
    awayScore = awayScore,
    homeTeamLogoUrl = homeTeamLogoUrl,
    awayTeamLogoUrl = awayTeamLogoUrl,
    roundLabel = roundLabel,
    venueAddress = venueAddress,
    latitude = latitude,
    longitude = longitude,
    role = com.github.apkelly.drool.common.domain.model.FixtureRole.valueOf(role.name),
    refereeRole = refereeRole,
    refereeAllocationId = refereeAllocationId,
    refereeStatus = refereeStatus,
    leagueName = leagueName,
    leagueId = leagueId,
    seasonId = seasonId,
    tenantId = tenantId,
)

internal fun com.github.apkelly.dribl.model.TeamHub.toDomain() = com.github.apkelly.drool.common.domain.model.TeamHub(
    teamId = teamId,
    matches = matches.map { it.toDomain() },
    results = results.map { it.toDomain() },
    ladderName = ladderName,
    ladder = ladder.map { it.toDomain() },
)

private fun com.github.apkelly.dribl.model.LadderEntry.toDomain() =
    com.github.apkelly.drool.common.domain.model.LadderEntry(
        position = position,
        teamId = teamId,
        teamName = teamName,
        logoUrl = logoUrl,
        played = played,
        won = won,
        drawn = drawn,
        lost = lost,
        goalsFor = goalsFor,
        goalsAgainst = goalsAgainst,
        goalDifference = goalDifference,
        points = points,
    )
