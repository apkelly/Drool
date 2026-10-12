package com.github.apkelly.drool.app.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.github.apkelly.drool.app.ui.viewmodel.AppViewModel
import com.github.apkelly.drool.common.observability.AppScreen
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.nav_discover
import com.github.apkelly.drool.common.resources.nav_home
import com.github.apkelly.drool.common.resources.nav_profile
import com.github.apkelly.drool.common.resources.nav_schedule
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbolIcon
import com.github.apkelly.drool.discover.ui.ClubScreen
import com.github.apkelly.drool.discover.ui.DiscoverScreen
import com.github.apkelly.drool.discover.ui.model.DiscoverUiModel
import com.github.apkelly.drool.discover.ui.viewmodel.DiscoverViewModel
import com.github.apkelly.drool.home.ui.HomeScreen
import com.github.apkelly.drool.home.ui.model.HomeUiModel
import com.github.apkelly.drool.match.ui.MatchDetailsScreen
import com.github.apkelly.drool.match.ui.TeamAvailabilityScreen
import com.github.apkelly.drool.match.ui.model.MatchDetailsUiState
import com.github.apkelly.drool.match.ui.model.MatchSheetUiState
import com.github.apkelly.drool.match.ui.viewmodel.MatchViewModel
import com.github.apkelly.drool.profile.ui.LinkMemberScreen
import com.github.apkelly.drool.profile.ui.PersonalInformationScreen
import com.github.apkelly.drool.profile.ui.ProfileScreen
import com.github.apkelly.drool.profile.ui.model.ProfileUiModel
import com.github.apkelly.drool.profile.ui.playerCardRole
import com.github.apkelly.drool.schedule.background.rememberAssignmentNotificationPermissionRequester
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityDraft
import com.github.apkelly.drool.schedule.ui.AvailabilityScreen
import com.github.apkelly.drool.schedule.ui.ScheduleScreen
import com.github.apkelly.drool.schedule.ui.model.AvailabilityUiModel
import com.github.apkelly.drool.schedule.ui.model.ScheduleUiModel
import com.github.apkelly.drool.schedule.ui.viewmodel.AvailabilityViewModel
import com.github.apkelly.drool.schedule.ui.viewmodel.ScheduleViewModel
import com.github.apkelly.drool.team.ui.TeamHubScreen
import com.github.apkelly.drool.team.ui.viewmodel.TeamViewModel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclassesOfSealed
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSerializationApi::class)
internal val navigationStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<AppRoute>()
        }
    }
}

@Composable
@Suppress("CyclomaticComplexMethod", "LongMethod")
fun MainNavigation(
    profile: com.github.apkelly.drool.common.domain.model.Profile,
    appViewModel: AppViewModel,
    discoverViewModel: DiscoverViewModel,
    scheduleViewModel: ScheduleViewModel,
    teamViewModel: TeamViewModel,
    matchViewModel: MatchViewModel,
    availabilityViewModel: AvailabilityViewModel,
    modifier: Modifier = Modifier,
) {
    val clubs by discoverViewModel.clubs.collectAsState()
    val teams by discoverViewModel.teams.collectAsState()
    val fixtures by scheduleViewModel.fixtures.collectAsState()
    val linkedProfiles by scheduleViewModel.linkedProfiles.collectAsState()
    val selectedProfileId by scheduleViewModel.selectedProfileId.collectAsState()
    val allocationUpdatingId by scheduleViewModel.allocationUpdatingId.collectAsState()
    val allocationFailure by scheduleViewModel.allocationFailure.collectAsState()
    val profileTeams by scheduleViewModel.profileTeams.collectAsState()
    val allProfileTeams by scheduleViewModel.allProfileTeams.collectAsState()
    val linkedProfileRefreshResults by
        scheduleViewModel.linkedProfileRefreshResults.collectAsState()
    val linkMemberState by appViewModel.linkMemberState.collectAsState()
    val unlinkMemberState by appViewModel.unlinkMemberState.collectAsState()
    val relationships by discoverViewModel.relationships.collectAsState()
    val themeMode by appViewModel.themeMode.collectAsState()
    val observabilityEnabled by appViewModel.observabilityEnabled.collectAsState()
    val assignmentPollingInterval by
        appViewModel.assignmentPollingInterval.collectAsState()
    val teamHubState by teamViewModel.state.collectAsState()
    val matchDetailsState by matchViewModel.state.collectAsState()
    val matchSheetState by matchViewModel.matchSheetState.collectAsState()
    val availabilityState by availabilityViewModel.state.collectAsState()
    val availabilityProfileId by availabilityViewModel.activeProfileId.collectAsState()
    val refereeProfileIds by availabilityViewModel.refereeProfileIds.collectAsState()
    val shouldRequestNotificationPermission by
        availabilityViewModel.shouldRequestNotificationPermission.collectAsState()
    val requestAssignmentNotificationPermission =
        rememberAssignmentNotificationPermissionRequester()

    val homeStack = rememberNavBackStack(navigationStateConfiguration, AppRoute.Home as NavKey)
    val scheduleStack = rememberNavBackStack(
        navigationStateConfiguration,
        AppRoute.Schedule as NavKey,
    )
    val discoverStack = rememberNavBackStack(
        navigationStateConfiguration,
        AppRoute.Discover as NavKey,
    )
    val profileStack = rememberNavBackStack(
        navigationStateConfiguration,
        AppRoute.Profile as NavKey,
    )
    var selected by remember { mutableStateOf(TopLevelDestination.Home) }
    val currentStack = when (selected) {
        TopLevelDestination.Home -> homeStack
        TopLevelDestination.Schedule -> scheduleStack
        TopLevelDestination.Discover -> discoverStack
        TopLevelDestination.Profile -> profileStack
    }
    LaunchedEffect(currentStack.lastOrNull(), selected) {
        val screen = when (currentStack.lastOrNull()) {
            AppRoute.Home -> AppScreen.Home
            AppRoute.Schedule -> AppScreen.Schedule
            AppRoute.Discover -> AppScreen.Discover
            AppRoute.Profile -> AppScreen.Profile
            is AppRoute.Club -> AppScreen.Club
            is AppRoute.Team -> AppScreen.Team
            is AppRoute.MatchDetails -> AppScreen.MatchDetails
            is AppRoute.TeamAvailability -> AppScreen.TeamAvailability
            is AppRoute.PersonalInformation -> AppScreen.PersonalInformation
            is AppRoute.Availability -> AppScreen.Availability
            AppRoute.LinkMember -> AppScreen.LinkMember
            else -> null
        }
        screen?.let(appViewModel::trackScreen)
    }

    val content: @Composable () -> Unit = {
        NavDisplay(
            backStack = currentStack,
            onBack = {
                if (currentStack.size > 1) currentStack.removeAt(currentStack.lastIndex)
            },
            entryProvider = entryProvider {
                entry<AppRoute.Home> {
                    HomeScreen(
                        model = HomeUiModel(
                            profile = profile,
                            linkedProfiles = linkedProfiles,
                            selectedProfileId = selectedProfileId,
                            profileTeams = profileTeams,
                            linkedProfileRefreshResults = linkedProfileRefreshResults,
                            fixtures = fixtures,
                        ),
                        onProfileSelect = scheduleViewModel::selectLinkedProfile,
                        onRefresh = scheduleViewModel::refreshFixtures,
                        onSignInAgain = appViewModel::signOut,
                        onFixtureSelect = { fixture ->
                            fixture.profileId?.let { profileId ->
                                currentStack.add(
                                    AppRoute.MatchDetails(
                                        profileId = profileId,
                                        matchId = fixture.id,
                                    ),
                                )
                            }
                        },
                        onViewSchedule = {
                            selected = TopLevelDestination.Schedule
                        },
                        onTeamSelect = { profileTeam ->
                            val team = profileTeam.team
                            currentStack.add(
                                AppRoute.Team(
                                    profileId = profileTeam.profileId,
                                    teamId = team.id,
                                    teamName = team.name,
                                    logoUrl = team.logoUrl,
                                    primaryColor = team.primaryColor,
                                    secondaryColor = team.secondaryColor,
                                ),
                            )
                        },
                    )
                }
                entry<AppRoute.Schedule> {
                    ScheduleScreen(
                        model = ScheduleUiModel(
                            fixtures = fixtures,
                            linkedProfiles = linkedProfiles,
                            selectedProfileId = selectedProfileId,
                            canManageRefereeAvailability =
                            selectedProfileId in refereeProfileIds,
                            allocationUpdatingId = allocationUpdatingId,
                            allocationFailure = allocationFailure,
                        ),
                        onProfileSelect = scheduleViewModel::selectLinkedProfile,
                        onRefresh = scheduleViewModel::refreshFixtures,
                        onFixtureSelect = { fixture ->
                            fixture.profileId?.let { profileId ->
                                currentStack.add(
                                    AppRoute.MatchDetails(
                                        profileId = profileId,
                                        matchId = fixture.id,
                                    ),
                                )
                            }
                        },
                        onAllocationDecision = scheduleViewModel::updateRefereeAllocation,
                        onOpenAvailability = {
                            selectedProfileId?.let { profileId ->
                                currentStack.add(AppRoute.Availability(profileId))
                            }
                        },
                    )
                }
                entry<AppRoute.Discover> {
                    DiscoverScreen(
                        model = DiscoverUiModel(clubs),
                        onRefresh = discoverViewModel::refreshClubs,
                        onClubSelect = { club ->
                            discoverViewModel.selectClub(club.id)
                            currentStack.add(AppRoute.Club(club.id))
                        },
                    )
                }
                entry<AppRoute.Club> { route ->
                    val club = clubs.items.firstOrNull { it.id == route.clubId }
                    if (club != null) {
                        ClubScreen(
                            club = club,
                            teams = teams,
                            relationships = relationships,
                            onBack = {
                                if (currentStack.size > 1) {
                                    currentStack.removeAt(currentStack.lastIndex)
                                }
                            },
                            onRefresh = discoverViewModel::refreshTeams,
                            onFollowingChange = discoverViewModel::setFollowing,
                            onTeamSelect = { team ->
                                currentStack.add(
                                    AppRoute.Team(
                                        profileId = profile.accountId,
                                        teamId = team.id,
                                        teamName = team.name,
                                        logoUrl = team.logoUrl,
                                        primaryColor = team.primaryColor,
                                        secondaryColor = team.secondaryColor,
                                    ),
                                )
                            },
                        )
                    }
                }
                entry<AppRoute.Team> { route ->
                    TeamHubScreen(
                        profileId = route.profileId,
                        teamId = route.teamId,
                        teamName = route.teamName,
                        logoUrl = route.logoUrl,
                        primaryColor = route.primaryColor,
                        secondaryColor = route.secondaryColor,
                        state = teamHubState,
                        onBack = {
                            if (currentStack.size > 1) {
                                currentStack.removeAt(currentStack.lastIndex)
                            }
                        },
                        onLoad = { profileId, teamId ->
                            teamViewModel.load(
                                profileId,
                                teamId,
                                route.competitionName,
                                route.leagueId,
                            )
                        },
                        onRefresh = { profileId, teamId ->
                            teamViewModel.refresh(
                                profileId,
                                teamId,
                                route.competitionName,
                                route.leagueId,
                            )
                        },
                        onMatchSelect = { fixture ->
                            currentStack.add(
                                AppRoute.MatchDetails(
                                    profileId = route.profileId,
                                    matchId = fixture.id,
                                ),
                            )
                        },
                    )
                }
                entry<AppRoute.MatchDetails> { route ->
                    val details = matchDetailsState as? MatchDetailsUiState.Content
                    val fixture = details?.takeIf {
                        it.profileId == route.profileId && it.matchId == route.matchId
                    }?.fixture
                    val managedTeamIds = allProfileTeams
                        .asSequence()
                        .filter { it.profileId == route.profileId }
                        .filter {
                            it.relationship ==
                                com.github.apkelly.drool.common.domain.model.TeamRelationship.Coaching ||
                                it.relationship ==
                                com.github.apkelly.drool.common.domain.model.TeamRelationship.Managing
                        }
                        .map { it.team.id }
                        .toSet()
                    val matchTeamIds = setOfNotNull(
                        fixture?.homeTeamId,
                        fixture?.awayTeamId,
                    )
                    val canViewMatchSheet =
                        fixture?.role ==
                            com.github.apkelly.drool.common.domain.model.FixtureRole.Referee ||
                            managedTeamIds.any(matchTeamIds::contains)
                    MatchDetailsScreen(
                        profileId = route.profileId,
                        matchId = route.matchId,
                        state = matchDetailsState,
                        onBack = {
                            if (currentStack.size > 1) {
                                currentStack.removeAt(currentStack.lastIndex)
                            }
                        },
                        onLoad = matchViewModel::load,
                        onRefresh = matchViewModel::refresh,
                        canViewMatchSheet = canViewMatchSheet,
                        matchSheetState = matchSheetState,
                        onLoadMatchSheet = matchViewModel::loadMatchSheet,
                        onMatchSheetTeamSelect = { teamId ->
                            currentStack.add(
                                AppRoute.TeamAvailability(
                                    profileId = route.profileId,
                                    matchId = route.matchId,
                                    teamId = teamId,
                                ),
                            )
                        },
                        onTeamSelect = {
                                teamId,
                                teamName,
                                logoUrl,
                                competitionName,
                                leagueId,
                            ->
                            currentStack.add(
                                AppRoute.Team(
                                    profileId = route.profileId,
                                    teamId = teamId,
                                    teamName = teamName,
                                    logoUrl = logoUrl,
                                    competitionName = competitionName,
                                    leagueId = leagueId,
                                ),
                            )
                        },
                    )
                }
                entry<AppRoute.TeamAvailability> { route ->
                    val details = matchDetailsState as? MatchDetailsUiState.Content
                    val fixture = details?.takeIf {
                        it.profileId == route.profileId && it.matchId == route.matchId
                    }?.fixture
                    val managedTeamIds = allProfileTeams
                        .asSequence()
                        .filter { it.profileId == route.profileId }
                        .filter {
                            it.relationship ==
                                com.github.apkelly.drool.common.domain.model.TeamRelationship.Coaching ||
                                it.relationship ==
                                com.github.apkelly.drool.common.domain.model.TeamRelationship.Managing
                        }
                        .map { it.team.id }
                        .toSet()
                    val isReferee = fixture?.role ==
                        com.github.apkelly.drool.common.domain.model.FixtureRole.Referee
                    val content = matchSheetState as? MatchSheetUiState.Content
                    val team = content?.matchSheet?.homeTeam?.takeIf {
                        it.id == route.teamId
                    } ?: content?.matchSheet?.awayTeam?.takeIf {
                        it.id == route.teamId
                    }
                    val managesMatchTeam = managedTeamIds.any {
                        it == fixture?.homeTeamId || it == fixture?.awayTeamId
                    }
                    val action = when {
                        isReferee || team == null -> null

                        route.teamId in managedTeamIds -> when (team.status) {
                            com.github.apkelly.drool.common.domain.model.TeamSheetStatus.Open ->
                                com.github.apkelly.drool.common.domain.model.TeamSheetAction.Submit

                            com.github.apkelly.drool.common.domain.model.TeamSheetStatus.Submitted ->
                                com.github.apkelly.drool.common.domain.model.TeamSheetAction.Unsubmit

                            com.github.apkelly.drool.common.domain.model.TeamSheetStatus.Confirmed ->
                                null
                        }

                        managesMatchTeam -> when (team.status) {
                            com.github.apkelly.drool.common.domain.model.TeamSheetStatus.Submitted ->
                                com.github.apkelly.drool.common.domain.model.TeamSheetAction.Confirm

                            com.github.apkelly.drool.common.domain.model.TeamSheetStatus.Confirmed ->
                                com.github.apkelly.drool.common.domain.model.TeamSheetAction.Unconfirm

                            com.github.apkelly.drool.common.domain.model.TeamSheetStatus.Open ->
                                null
                        }

                        else -> null
                    }
                    TeamAvailabilityScreen(
                        teamId = route.teamId,
                        state = matchSheetState,
                        canEditMembers = isReferee || route.teamId in managedTeamIds,
                        teamAction = action,
                        onBack = {
                            if (currentStack.size > 1) {
                                currentStack.removeAt(currentStack.lastIndex)
                            }
                        },
                        onAvailabilityChange = { memberId, available ->
                            matchViewModel.updateMemberAvailability(
                                route.profileId,
                                memberId,
                                available,
                            )
                        },
                        onTeamAction = { teamAction ->
                            matchViewModel.updateTeamStatus(
                                route.profileId,
                                route.teamId,
                                teamAction,
                            )
                        },
                    )
                }
                entry<AppRoute.Profile> {
                    ProfileScreen(
                        model = ProfileUiModel(
                            profile = profile,
                            themeMode = themeMode,
                            observabilityEnabled = observabilityEnabled,
                            assignmentPollingInterval = assignmentPollingInterval,
                            unlinkMemberState = unlinkMemberState,
                        ),
                        onThemeChange = appViewModel::updateTheme,
                        onObservabilityChange = appViewModel::updateObservability,
                        onAssignmentPollingIntervalChange =
                        appViewModel::updateAssignmentPollingInterval,
                        onAddMember = {
                            currentStack.add(AppRoute.LinkMember)
                        },
                        onRelatedUserSelect = { user ->
                            currentStack.add(AppRoute.PersonalInformation(user.subjectUserId))
                        },
                        onUnlinkUser = appViewModel::unlinkLinkedUser,
                        onClearUnlinkFailure = appViewModel::clearUnlinkMemberFailure,
                        onSignOut = appViewModel::signOut,
                    )
                }
                entry<AppRoute.PersonalInformation> { route ->
                    profile.relatedUsers
                        .firstOrNull {
                            it.isLinked && it.subjectUserId == route.userId
                        }
                        ?.let { user ->
                            PersonalInformationScreen(
                                user = user,
                                cardRole = playerCardRole(
                                    allProfileTeams
                                        .filter { it.profileId == route.userId }
                                        .map { it.relationship },
                                ),
                                onBack = {
                                    if (currentStack.size > 1) {
                                        currentStack.removeAt(currentStack.lastIndex)
                                    }
                                },
                                availabilityContent = if (
                                    route.userId in refereeProfileIds
                                ) {
                                    {
                                        AvailabilityDestination(
                                            model = AvailabilityUiModel(
                                                profileId = route.userId,
                                                profileName = user.displayName,
                                                state = if (
                                                    availabilityProfileId == route.userId
                                                ) {
                                                    availabilityState
                                                } else {
                                                    com.github.apkelly.drool.schedule.ui.model
                                                        .AvailabilityUiState.Idle
                                                },
                                            ),
                                            onLoad = {
                                                availabilityViewModel.load(route.userId)
                                            },
                                            onRefresh = {
                                                availabilityViewModel.load(
                                                    route.userId,
                                                    force = true,
                                                )
                                            },
                                            onCreate = { draft ->
                                                availabilityViewModel.create(
                                                    route.userId,
                                                    draft,
                                                )
                                            },
                                            shouldRequestNotificationPermission =
                                            shouldRequestNotificationPermission,
                                            onNotificationPermissionRequestConsume = {
                                                availabilityViewModel
                                                    .consumeNotificationPermissionRequest()
                                            },
                                            onRequestNotificationPermission =
                                            requestAssignmentNotificationPermission,
                                            onDelete = { availabilityId ->
                                                availabilityViewModel.delete(
                                                    route.userId,
                                                    availabilityId,
                                                )
                                            },
                                            onClearFailure =
                                            availabilityViewModel::clearFailure,
                                            onBack = {},
                                            showTopBar = false,
                                        )
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                }
                entry<AppRoute.Availability> { route ->
                    AvailabilityDestination(
                        model = AvailabilityUiModel(
                            profileId = route.profileId,
                            profileName = linkedProfiles
                                .firstOrNull { it.id == route.profileId }
                                ?.displayName
                                .orEmpty(),
                            state = if (availabilityProfileId == route.profileId) {
                                availabilityState
                            } else {
                                com.github.apkelly.drool.schedule.ui.model.AvailabilityUiState.Idle
                            },
                        ),
                        onLoad = { availabilityViewModel.load(route.profileId) },
                        onRefresh = {
                            availabilityViewModel.load(route.profileId, force = true)
                        },
                        onCreate = { draft ->
                            availabilityViewModel.create(route.profileId, draft)
                        },
                        shouldRequestNotificationPermission =
                        shouldRequestNotificationPermission,
                        onNotificationPermissionRequestConsume =
                        availabilityViewModel::consumeNotificationPermissionRequest,
                        onRequestNotificationPermission =
                        requestAssignmentNotificationPermission,
                        onDelete = { availabilityId ->
                            availabilityViewModel.delete(route.profileId, availabilityId)
                        },
                        onClearFailure = availabilityViewModel::clearFailure,
                        onBack = {
                            if (currentStack.size > 1) {
                                currentStack.removeAt(currentStack.lastIndex)
                            }
                        },
                    )
                }
                entry<AppRoute.LinkMember> {
                    LinkMemberScreen(
                        state = linkMemberState,
                        onBack = {
                            if (currentStack.size > 1) {
                                currentStack.removeAt(currentStack.lastIndex)
                            }
                        },
                        onLoad = appViewModel::loadLinkCandidates,
                        onRequest = appViewModel::requestLinkedUser,
                        onVerify = appViewModel::verifyLinkedUser,
                        onCancelVerification = appViewModel::cancelLinkVerification,
                        onLink = {
                            appViewModel.completeLinkMemberFlow()
                            if (currentStack.size > 1) {
                                currentStack.removeAt(currentStack.lastIndex)
                            }
                        },
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        topLevelItems().forEach { item ->
                            NavigationBarItem(
                                selected = selected == item.destination,
                                onClick = { selected = item.destination },
                                icon = {
                                    MaterialSymbolIcon(
                                        item.symbol,
                                        contentDescription = null,
                                    )
                                },
                                label = { Text(stringResource(item.label)) },
                            )
                        }
                    }
                },
            ) { contentPadding ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .consumeWindowInsets(contentPadding),
                ) {
                    content()
                }
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    topLevelItems().forEach { item ->
                        NavigationRailItem(
                            selected = selected == item.destination,
                            onClick = { selected = item.destination },
                            icon = {
                                MaterialSymbolIcon(
                                    item.symbol,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun AvailabilityDestination(
    model: AvailabilityUiModel,
    onLoad: () -> Unit,
    onRefresh: () -> Unit,
    onCreate: (RefereeAvailabilityDraft) -> Unit,
    shouldRequestNotificationPermission: Boolean,
    onNotificationPermissionRequestConsume: () -> Unit,
    onRequestNotificationPermission: (() -> Unit)?,
    onDelete: (String) -> Unit,
    onClearFailure: () -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    AvailabilityScreen(
        model = model,
        onBack = onBack,
        onLoad = onLoad,
        onRefresh = onRefresh,
        onCreate = onCreate,
        shouldRequestNotificationPermission = shouldRequestNotificationPermission,
        onNotificationPermissionRequestConsume =
        onNotificationPermissionRequestConsume,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onDelete = onDelete,
        onClearFailure = onClearFailure,
        showTopBar = showTopBar,
    )
}

private data class TopLevelItem(
    val destination: TopLevelDestination,
    val label: StringResource,
    val symbol: MaterialSymbol,
)

private fun topLevelItems() = listOf(
    TopLevelItem(TopLevelDestination.Home, Res.string.nav_home, MaterialSymbol.Home),
    TopLevelItem(
        TopLevelDestination.Schedule,
        Res.string.nav_schedule,
        MaterialSymbol.CalendarMonth,
    ),
    TopLevelItem(TopLevelDestination.Discover, Res.string.nav_discover, MaterialSymbol.Search),
    TopLevelItem(TopLevelDestination.Profile, Res.string.nav_profile, MaterialSymbol.Person),
)
