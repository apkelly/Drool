package com.github.apkelly.drool.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclassesOfSealed
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.nav_discover
import com.github.apkelly.drool.resources.nav_home
import com.github.apkelly.drool.resources.nav_profile
import com.github.apkelly.drool.resources.nav_schedule
import com.github.apkelly.drool.ui.model.AuthFailure
import com.github.apkelly.drool.ui.model.SessionUiState
import com.github.apkelly.drool.ui.navigation.AppRoute
import com.github.apkelly.drool.ui.navigation.TopLevelDestination
import com.github.apkelly.drool.ui.viewmodel.AppViewModel
import com.github.apkelly.drool.ui.viewmodel.SportsViewModel
import com.github.apkelly.drool.ui.widgets.SplashContent
import com.github.apkelly.drool.observability.AppScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSerializationApi::class)
private val navigationStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<AppRoute>()
        }
    }
}

@Composable
fun DroolApp(
    appViewModel: AppViewModel,
    sportsViewModel: SportsViewModel,
) {
    val sessionState by appViewModel.sessionState.collectAsState()
    var initialSplashFinished by rememberSaveable { mutableStateOf(false) }
    if (!initialSplashFinished || sessionState == SessionUiState.Bootstrapping) {
        SplashContent(onAnimationFinished = { initialSplashFinished = true })
        return
    }
    when (val state = sessionState) {
        SessionUiState.Bootstrapping -> Unit
        SessionUiState.ReconnectRequired -> ReconnectScreen(appViewModel::restore)
        SessionUiState.AuthenticationRequired,
        SessionUiState.Authenticating,
        is SessionUiState.AuthenticationFailed,
        -> AuthNavigation(
            sessionState = state,
            onSignIn = appViewModel::signIn,
            onScreenViewed = appViewModel::trackScreen,
        )
        is SessionUiState.Authenticated -> {
            LaunchedEffect(state.profile.accountId) {
                sportsViewModel.refreshAll(force = false)
            }
            MainNavigation(
                profile = state.profile,
                appViewModel = appViewModel,
                sportsViewModel = sportsViewModel,
            )
        }
    }
}

@Composable
private fun AuthNavigation(
    sessionState: SessionUiState,
    onSignIn: (String, String) -> Unit,
    onScreenViewed: (AppScreen) -> Unit,
) {
    val backStack = rememberNavBackStack(
        navigationStateConfiguration,
        AppRoute.Welcome as NavKey,
    )
    LaunchedEffect(backStack.lastOrNull()) {
        onScreenViewed(
            when (backStack.lastOrNull()) {
                AppRoute.SignIn -> AppScreen.SignIn
                AppRoute.Register -> AppScreen.Register
                else -> AppScreen.Welcome
            }
        )
    }
    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        },
        entryProvider = entryProvider {
            entry<AppRoute.Welcome> {
                WelcomeScreen(
                    onSignIn = { backStack.add(AppRoute.SignIn) },
                    onCreateAccount = { backStack.add(AppRoute.Register) },
                )
            }
            entry<AppRoute.SignIn> {
                SignInScreen(
                    loading = sessionState == SessionUiState.Authenticating,
                    failure = (sessionState as? SessionUiState.AuthenticationFailed)?.reason,
                    onBack = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    },
                    onSignIn = onSignIn,
                )
            }
            entry<AppRoute.Register> {
                RegisterScreen(
                    onBack = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }
        },
    )
}

@Composable
private fun MainNavigation(
    profile: com.github.apkelly.drool.domain.model.Profile,
    appViewModel: AppViewModel,
    sportsViewModel: SportsViewModel,
) {
    val clubs by sportsViewModel.clubs.collectAsState()
    val teams by sportsViewModel.teams.collectAsState()
    val fixtures by sportsViewModel.fixtures.collectAsState()
    val familyProfiles by sportsViewModel.familyProfiles.collectAsState()
    val selectedProfileId by sportsViewModel.selectedProfileId.collectAsState()
    val familyTeams by sportsViewModel.familyTeams.collectAsState()
    val familyClubs by sportsViewModel.familyClubs.collectAsState()
    val familyRefreshResults by sportsViewModel.familyRefreshResults.collectAsState()
    val linkMemberState by appViewModel.linkMemberState.collectAsState()
    val relationships by sportsViewModel.relationships.collectAsState()
    val themeMode by appViewModel.themeMode.collectAsState()
    val observabilityEnabled by appViewModel.observabilityEnabled.collectAsState()
    val teamHubState by sportsViewModel.teamHubState.collectAsState()
    val matchDetailsState by sportsViewModel.matchDetailsState.collectAsState()

    val configuredFamily = profile.familyProfiles
    LaunchedEffect(profile.accountId, configuredFamily) {
        sportsViewModel.configureFamily(profile)
        sportsViewModel.refreshFixtures(force = false)
    }

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
            is AppRoute.PersonalInformation -> AppScreen.PersonalInformation
            AppRoute.LinkMember -> AppScreen.LinkMember
            AppRoute.ApiDiagnostics -> AppScreen.ApiDiagnostics
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
                        profile = profile,
                        familyProfiles = familyProfiles,
                        selectedProfileId = selectedProfileId,
                        familyTeams = familyTeams,
                        familyClubs = familyClubs,
                        familyRefreshResults = familyRefreshResults,
                        onProfileSelected = sportsViewModel::selectFamilyProfile,
                        fixtures = fixtures,
                        onRefresh = sportsViewModel::refreshFixtures,
                        onSignInAgain = appViewModel::signOut,
                        onFixtureSelected = { fixture ->
                            fixture.profileId?.let { profileId ->
                                currentStack.add(
                                    AppRoute.MatchDetails(
                                        profileId = profileId,
                                        matchId = fixture.id,
                                    )
                                )
                            }
                        },
                        onTeamSelected = { familyTeam ->
                            val team = familyTeam.team
                            currentStack.add(
                                AppRoute.Team(
                                    profileId = familyTeam.profileId,
                                    teamId = team.id,
                                    teamName = team.name,
                                    logoUrl = team.logoUrl,
                                    primaryColor = team.primaryColor,
                                    secondaryColor = team.secondaryColor,
                                )
                            )
                        },
                    )
                }
                entry<AppRoute.Schedule> {
                    ScheduleScreen(
                        state = fixtures,
                        familyProfiles = familyProfiles,
                        selectedProfileId = selectedProfileId,
                        onProfileSelected = sportsViewModel::selectFamilyProfile,
                        onRefresh = sportsViewModel::refreshFixtures,
                        onFixtureSelected = { fixture ->
                            fixture.profileId?.let { profileId ->
                                currentStack.add(
                                    AppRoute.MatchDetails(
                                        profileId = profileId,
                                        matchId = fixture.id,
                                    )
                                )
                            }
                        },
                    )
                }
                entry<AppRoute.Discover> {
                    DiscoverScreen(
                        state = clubs,
                        onRefresh = sportsViewModel::refreshClubs,
                        onClubSelected = { club ->
                            sportsViewModel.selectClub(club.id)
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
                            onRefresh = sportsViewModel::refreshTeams,
                            onFollowingChanged = sportsViewModel::setFollowing,
                            onTeamSelected = { team ->
                                currentStack.add(
                                    AppRoute.Team(
                                        profileId = profile.accountId,
                                        teamId = team.id,
                                        teamName = team.name,
                                        logoUrl = team.logoUrl,
                                        primaryColor = team.primaryColor,
                                        secondaryColor = team.secondaryColor,
                                    )
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
                        onLoad = sportsViewModel::loadTeam,
                        onRefresh = sportsViewModel::refreshTeam,
                        onMatchSelected = { fixture ->
                            currentStack.add(
                                AppRoute.MatchDetails(
                                    profileId = route.profileId,
                                    matchId = fixture.id,
                                )
                            )
                        },
                    )
                }
                entry<AppRoute.MatchDetails> { route ->
                    MatchDetailsScreen(
                        profileId = route.profileId,
                        matchId = route.matchId,
                        state = matchDetailsState,
                        onBack = {
                            if (currentStack.size > 1) {
                                currentStack.removeAt(currentStack.lastIndex)
                            }
                        },
                        onLoad = sportsViewModel::loadMatch,
                        onTeamSelected = { teamId, teamName, logoUrl ->
                            currentStack.add(
                                AppRoute.Team(
                                    profileId = route.profileId,
                                    teamId = teamId,
                                    teamName = teamName,
                                    logoUrl = logoUrl,
                                )
                            )
                        },
                    )
                }
                entry<AppRoute.Profile> {
                    ProfileScreen(
                        profile = profile,
                        themeMode = themeMode,
                        observabilityEnabled = observabilityEnabled,
                        onThemeChanged = appViewModel::updateTheme,
                        onObservabilityChanged = appViewModel::updateObservability,
                        onOpenApiDiagnostics = {
                            currentStack.add(AppRoute.ApiDiagnostics)
                        },
                        onAddMember = {
                            currentStack.add(AppRoute.LinkMember)
                        },
                        onRelatedUserSelected = { user ->
                            currentStack.add(AppRoute.PersonalInformation(user.subjectUserId))
                        },
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
                                onBack = {
                                    if (currentStack.size > 1) {
                                        currentStack.removeAt(currentStack.lastIndex)
                                    }
                                },
                            )
                        }
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
                        onVerify = appViewModel::verifyLinkedUser,
                        onLinked = appViewModel::restore,
                    )
                }
                entry<AppRoute.ApiDiagnostics> {
                    ApiDiagnosticsScreen(
                        onBack = {
                            if (currentStack.size > 1) {
                                currentStack.removeAt(currentStack.lastIndex)
                            }
                        },
                        onLinkedUsers = appViewModel::refreshLinkedUsers,
                        onClubs = { sportsViewModel.refreshClubs() },
                        onTeams = { sportsViewModel.refreshTeams() },
                        onSchedule = { sportsViewModel.refreshFixtures() },
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        topLevelItems().forEach { item ->
                            NavigationBarItem(
                                selected = selected == item.destination,
                                onClick = { selected = item.destination },
                                icon = { Icon(item.icon, contentDescription = null) },
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
                            icon = { Icon(item.icon, contentDescription = null) },
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

private data class TopLevelItem(
    val destination: TopLevelDestination,
    val label: StringResource,
    val icon: ImageVector,
)

private fun topLevelItems() = listOf(
    TopLevelItem(TopLevelDestination.Home, Res.string.nav_home, Icons.Default.Home),
    TopLevelItem(TopLevelDestination.Schedule, Res.string.nav_schedule, Icons.Default.CalendarMonth),
    TopLevelItem(TopLevelDestination.Discover, Res.string.nav_discover, Icons.Default.Search),
    TopLevelItem(TopLevelDestination.Profile, Res.string.nav_profile, Icons.Default.Person),
)
