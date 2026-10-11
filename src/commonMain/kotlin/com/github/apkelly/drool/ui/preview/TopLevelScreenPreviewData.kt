package com.github.apkelly.drool.ui.preview

import com.github.apkelly.drool.discover.ui.model.DiscoverUiModel
import com.github.apkelly.drool.domain.model.Account
import com.github.apkelly.drool.domain.model.Club
import com.github.apkelly.drool.domain.model.FamilyTeam
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FixtureRole
import com.github.apkelly.drool.domain.model.FixtureStatus
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.RelatedUser
import com.github.apkelly.drool.domain.model.Team
import com.github.apkelly.drool.domain.model.TeamRelationship
import com.github.apkelly.drool.domain.model.ThemeMode
import com.github.apkelly.drool.home.ui.model.HomeUiModel
import com.github.apkelly.drool.profile.ui.model.ProfileUiModel
import com.github.apkelly.drool.schedule.ui.model.ScheduleUiModel
import com.github.apkelly.drool.ui.model.CollectionUiState

internal object TopLevelScreenPreviewData {
    private val child = RelatedUser(
        id = "ethan",
        displayName = "Ethan Kelly",
        email = "ethan@example.com",
        avatarUrl = null,
        subjectUserId = "ethan",
        isLinked = true,
    )

    private val profile = Profile(
        accountId = "andrew",
        displayName = "Andrew Kelly",
        email = "andrew@example.com",
        relatedUsers = listOf(child),
        accounts = listOf(
            Account(
                id = "marrickville",
                name = "Marrickville FC",
                subtitle = "Coach",
                logoUrl = null,
            ),
        ),
    )

    private val coachingTeam = Team(
        id = "u13",
        clubId = "marrickville",
        name = "Sheffield Wednesday",
        shortName = null,
        logoUrl = null,
        ageGroup = "Under 13 Mixed",
        competitionName = "Marrickville Under 13 Mixed",
        active = true,
    )

    private val playerTeam = Team(
        id = "u6",
        clubId = "marrickville",
        name = "La Masia FC",
        shortName = null,
        logoUrl = null,
        ageGroup = "Under 6 Mixed",
        competitionName = "Marrickville Under 6 Mixed",
        active = true,
    )

    private val fixtures = listOf(
        Fixture(
            id = "player-match",
            profileId = "ethan",
            kickoffEpochMillis = 1_791_348_400_000,
            homeTeamId = "u13",
            homeTeamName = "Sheffield Wednesday",
            awayTeamId = "opponent",
            awayTeamName = "Cobh RAMBLERS",
            competitionName = "Under 13 Mixed",
            venueName = "Mackey Park Field 1",
            userTeamId = "u13",
            status = FixtureStatus.Scheduled,
            role = FixtureRole.Player,
        ),
        Fixture(
            id = "referee-match",
            profileId = "ethan",
            kickoffEpochMillis = 1_791_435_600_000,
            homeTeamId = "home",
            homeTeamName = "La Masia FC",
            awayTeamId = "away",
            awayTeamName = "The Strongest FC",
            competitionName = "Under 6 Mixed",
            venueName = "Fraser Park Field 2",
            userTeamId = null,
            status = FixtureStatus.Scheduled,
            role = FixtureRole.Referee,
            refereeRole = "Referee",
        ),
    )

    val home = HomeUiModel(
        profile = profile,
        familyProfiles = profile.familyProfiles,
        selectedProfileId = null,
        familyTeams = listOf(
            FamilyTeam("andrew", coachingTeam, TeamRelationship.Coaching),
            FamilyTeam("ethan", playerTeam, TeamRelationship.Player),
        ),
        familyRefreshResults = emptyMap(),
        fixtures = CollectionUiState(items = fixtures, isStale = false),
    )

    val schedule = ScheduleUiModel(
        fixtures = CollectionUiState(items = fixtures, isStale = false),
        familyProfiles = profile.familyProfiles,
        selectedProfileId = null,
    )

    val discover = DiscoverUiModel(
        clubs = CollectionUiState(
            items = listOf(
                Club(
                    id = "marrickville",
                    name = "Marrickville FC",
                    shortName = "MFC",
                    logoUrl = null,
                    primaryColor = "#006948",
                    secondaryColor = "#FFFFFF",
                ),
                Club(
                    id = "fraser",
                    name = "Fraser Park FC",
                    shortName = null,
                    logoUrl = null,
                    primaryColor = "#3559A8",
                    secondaryColor = "#FFFFFF",
                ),
            ),
            isStale = false,
        ),
    )

    val profileModel = ProfileUiModel(
        profile = profile,
        themeMode = ThemeMode.System,
        observabilityEnabled = false,
    )
}
