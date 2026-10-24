package com.github.apkelly.drool.match.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import com.github.apkelly.drool.common.domain.model.MatchSheetMember
import com.github.apkelly.drool.common.domain.model.MatchWeather
import com.github.apkelly.drool.common.domain.model.RefereeAllocationDecision
import com.github.apkelly.drool.common.domain.model.RefreshFailure
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_retry
import com.github.apkelly.drool.common.resources.content_team_logo
import com.github.apkelly.drool.common.resources.fixture_competition
import com.github.apkelly.drool.common.resources.fixture_role_referee
import com.github.apkelly.drool.common.resources.fixture_role_referee_named
import com.github.apkelly.drool.common.resources.match_details_tab
import com.github.apkelly.drool.common.resources.match_details_title
import com.github.apkelly.drool.common.resources.match_directions
import com.github.apkelly.drool.common.resources.match_kick_off
import com.github.apkelly.drool.common.resources.match_load_failed
import com.github.apkelly.drool.common.resources.match_round
import com.github.apkelly.drool.common.resources.match_round_unknown
import com.github.apkelly.drool.common.resources.match_score
import com.github.apkelly.drool.common.resources.match_sheet_tab
import com.github.apkelly.drool.common.resources.match_status_cancelled
import com.github.apkelly.drool.common.resources.match_status_completed
import com.github.apkelly.drool.common.resources.match_status_live
import com.github.apkelly.drool.common.resources.match_status_pending
import com.github.apkelly.drool.common.resources.match_status_postponed
import com.github.apkelly.drool.common.resources.match_status_scheduled
import com.github.apkelly.drool.common.resources.match_status_unknown
import com.github.apkelly.drool.common.resources.match_status_washout
import com.github.apkelly.drool.common.resources.match_venue
import com.github.apkelly.drool.common.resources.match_weather
import com.github.apkelly.drool.common.resources.match_weather_humidity
import com.github.apkelly.drool.common.resources.match_weather_loading
import com.github.apkelly.drool.common.resources.match_weather_rain
import com.github.apkelly.drool.common.resources.match_weather_sunscreen
import com.github.apkelly.drool.common.resources.match_weather_temperature
import com.github.apkelly.drool.common.resources.match_weather_unavailable
import com.github.apkelly.drool.common.resources.match_weather_uv
import com.github.apkelly.drool.common.resources.match_weather_washout
import com.github.apkelly.drool.common.resources.referee_checklist_tab
import com.github.apkelly.drool.common.resources.schedule_allocation_section_title
import com.github.apkelly.drool.common.resources.schedule_allocation_update_failed
import com.github.apkelly.drool.common.ui.format.displayTeamNames
import com.github.apkelly.drool.common.ui.format.formatFixtureDate
import com.github.apkelly.drool.common.ui.format.formatFixtureDay
import com.github.apkelly.drool.common.ui.format.formatFixtureTime
import com.github.apkelly.drool.common.ui.platform.GoogleMapPreview
import com.github.apkelly.drool.common.ui.platform.rememberLocationActionLauncher
import com.github.apkelly.drool.common.ui.widgets.CacheStatus
import com.github.apkelly.drool.common.ui.widgets.MaterialBackIcon
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbolIcon
import com.github.apkelly.drool.common.ui.widgets.PendingAllocationDecision
import com.github.apkelly.drool.common.ui.widgets.RefereeAllocationActions
import com.github.apkelly.drool.common.ui.widgets.RefereeAllocationConfirmationDialog
import com.github.apkelly.drool.common.ui.widgets.RefereeStatusPill
import com.github.apkelly.drool.common.ui.widgets.RemoteImage
import com.github.apkelly.drool.common.ui.widgets.SportsCard
import com.github.apkelly.drool.common.ui.widgets.canAcceptRefereeAllocation
import com.github.apkelly.drool.common.ui.widgets.canDeclineRefereeAllocation
import com.github.apkelly.drool.common.ui.widgets.hasAssignedRefereeAllocation
import com.github.apkelly.drool.match.domain.model.RefereeMatchPreset
import com.github.apkelly.drool.match.ui.model.MatchDetailsUiState
import com.github.apkelly.drool.match.ui.model.MatchSheetUiState
import com.github.apkelly.drool.match.ui.platform.platformWeatherTemperatureUnit
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
fun MatchDetailsScreen(
    profileId: String,
    matchId: String,
    state: MatchDetailsUiState,
    onBack: () -> Unit,
    onLoad: (String, String) -> Unit,
    onRefresh: (String, String) -> Unit,
    canViewMatchSheet: Boolean,
    matchSheetState: MatchSheetUiState,
    onLoadMatchSheet: (String, Boolean) -> Unit,
    onMatchSheetTeamSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    allocationUpdatingId: String? = null,
    allocationFailure: RefreshFailure? = null,
    onAllocationDecision: (Fixture, RefereeAllocationDecision) -> Unit = { _, _ -> },
    onCheckInReferee: (String, String) -> Unit = { _, _ -> },
    onSelectRefereePreset: (String, RefereeMatchPreset) -> Unit = { _, _ -> },
    onToggleRefereeTimer: (String, Long) -> Unit = { _, _ -> },
    onAdvanceRefereePhase: (String, Long) -> Unit = { _, _ -> },
    onResetRefereeTimer: (String) -> Unit = {},
    onRecordRefereeGoal: (String, Boolean, MatchSheetMember?, Boolean, Long) -> Unit =
        { _, _, _, _, _ -> },
    onRemoveRefereeGoal: (String, String) -> Unit = { _, _ -> },
    onSubmitRefereeReport: (String, Long) -> Unit = { _, _ -> },
    currentEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
    initialSelectedTab: Int = 0,
    onTeamSelect: (String, String, String?, String?, String?) -> Unit,
) {
    val currentOnLoad by rememberUpdatedState(onLoad)
    LaunchedEffect(profileId, matchId) { currentOnLoad(profileId, matchId) }
    val details = (state as? MatchDetailsUiState.Content)
        ?.takeIf { it.profileId == profileId && it.matchId == matchId }
    val showRefereeChecklist = shouldShowRefereeChecklistTab(
        fixture = details?.fixture,
        nowEpochMillis = currentEpochMillis,
    )
    var selectedTab by remember(profileId, matchId) { mutableIntStateOf(initialSelectedTab) }
    LaunchedEffect(showRefereeChecklist) {
        if (!showRefereeChecklist && selectedTab == 2) {
            selectedTab = 0
        }
    }
    var pendingDecision by remember { mutableStateOf<PendingAllocationDecision?>(null) }
    val currentOnLoadMatchSheet by rememberUpdatedState(onLoadMatchSheet)
    LaunchedEffect(profileId, details?.fixture?.matchSheetId, canViewMatchSheet) {
        if (canViewMatchSheet && details?.fixture?.matchSheetId != null) {
            currentOnLoadMatchSheet(profileId, false)
        }
    }
    RefereeAllocationConfirmationDialog(
        pending = pendingDecision,
        onDismiss = { pendingDecision = null },
        onConfirm = onAllocationDecision,
    )
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.match_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MaterialBackIcon(contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (canViewMatchSheet && details?.fixture?.matchSheetId != null) {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(Res.string.match_details_tab)) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(Res.string.match_sheet_tab)) },
                    )
                    if (showRefereeChecklist) {
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text(stringResource(Res.string.referee_checklist_tab)) },
                        )
                    }
                }
            }
            PullToRefreshBox(
                isRefreshing = details?.isRefreshing == true,
                onRefresh = {
                    if (selectedTab == 1 || selectedTab == 2) {
                        onLoadMatchSheet(profileId, true)
                    } else {
                        onRefresh(profileId, matchId)
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                when {
                    state is MatchDetailsUiState.Failed &&
                        state.profileId == profileId && state.matchId == matchId -> {
                        Column(Modifier.fillMaxSize()) {
                            LoadFailure(
                                message = stringResource(Res.string.match_load_failed),
                                onRetry = { onLoad(profileId, matchId) },
                            )
                        }
                    }

                    details == null -> {
                        Column(Modifier.fillMaxSize()) {
                            LoadingContent()
                        }
                    }

                    selectedTab == 1 && canViewMatchSheet -> MatchSheetSummary(
                        profileId = profileId,
                        matchId = matchId,
                        state = matchSheetState,
                        onRetry = { onLoadMatchSheet(profileId, true) },
                        onTeamSelect = onMatchSheetTeamSelect,
                    )

                    selectedTab == 2 && showRefereeChecklist -> RefereeChecklistTab(
                        profileId = profileId,
                        matchId = matchId,
                        fixture = details.fixture,
                        state = matchSheetState,
                        onRetry = { onLoadMatchSheet(profileId, true) },
                        onCheckInReferee = { refereeOfficialId ->
                            onCheckInReferee(profileId, refereeOfficialId)
                        },
                        onSelectPreset = { preset ->
                            onSelectRefereePreset(profileId, preset)
                        },
                        onToggleTimer = { nowMillis ->
                            onToggleRefereeTimer(profileId, nowMillis)
                        },
                        onAdvancePhase = { nowMillis ->
                            onAdvanceRefereePhase(profileId, nowMillis)
                        },
                        onResetTimer = {
                            onResetRefereeTimer(profileId)
                        },
                        onRecordGoal = { isHomeScoringTeam, member, ownGoal, nowMillis ->
                            onRecordRefereeGoal(
                                profileId,
                                isHomeScoringTeam,
                                member,
                                ownGoal,
                                nowMillis,
                            )
                        },
                        onRemoveGoal = { goalId ->
                            onRemoveRefereeGoal(profileId, goalId)
                        },
                        onSubmitReport = { nowMillis ->
                            onSubmitRefereeReport(profileId, nowMillis)
                        },
                    )

                    else -> MatchDetails(
                        fixture = details.fixture,
                        weather = details.weather,
                        isWeatherLoading = details.isWeatherLoading,
                        weatherUnavailable = details.weatherUnavailable,
                        refreshFailed = details.refreshFailed,
                        allocationUpdatingId = allocationUpdatingId,
                        allocationFailure = allocationFailure,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        onRefresh = { onRefresh(profileId, matchId) },
                        onRequestDecision = { pendingDecision = it },
                        onTeamSelect = onTeamSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchDetails(
    fixture: Fixture,
    weather: MatchWeather?,
    isWeatherLoading: Boolean,
    weatherUnavailable: Boolean,
    refreshFailed: Boolean,
    allocationUpdatingId: String?,
    allocationFailure: RefreshFailure?,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onRefresh: () -> Unit,
    onRequestDecision: (PendingAllocationDecision) -> Unit,
    onTeamSelect: (String, String, String?, String?, String?) -> Unit,
) {
    val locationLauncher = rememberLocationActionLauncher()
    val teamNames = fixture.displayTeamNames()
    val location = fixture.venueAddress ?: fixture.venueName.orEmpty()
    val round = fixture.roundLabel?.let {
        if (it.startsWith("round", ignoreCase = true)) {
            it
        } else {
            stringResource(Res.string.match_round, it)
        }
    } ?: stringResource(Res.string.match_round_unknown)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (refreshFailed) {
            item {
                CacheStatus(
                    isStale = false,
                    failure = RefreshFailure.Unknown,
                    hasContent = true,
                    onRetry = onRefresh,
                )
            }
        }
        if (fixture.role == FixtureRole.Referee) {
            item {
                RefereeAppointmentSection(
                    fixture = fixture,
                    allocationUpdatingId = allocationUpdatingId,
                    allocationFailure = allocationFailure,
                    onRequestDecision = onRequestDecision,
                )
            }
        }
        item {
            SportsCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                text = round,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = when (fixture.status) {
                                FixtureStatus.Washout,
                                FixtureStatus.Cancelled,
                                FixtureStatus.Postponed,
                                -> MaterialTheme.colorScheme.errorContainer

                                FixtureStatus.Live -> MaterialTheme.colorScheme.tertiaryContainer

                                else -> MaterialTheme.colorScheme.surfaceContainerLowest.copy(
                                    alpha = 0.8f,
                                )
                            },
                            contentColor = when (fixture.status) {
                                FixtureStatus.Washout,
                                FixtureStatus.Cancelled,
                                FixtureStatus.Postponed,
                                -> MaterialTheme.colorScheme.onErrorContainer

                                FixtureStatus.Live -> MaterialTheme.colorScheme.onTertiaryContainer

                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        ) {
                            Text(
                                text = fixtureStatusLabel(fixture.status),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MatchTeam(
                            fixture.homeTeamId,
                            fixture.homeTeamName,
                            teamNames.home,
                            fixture.homeTeamLogoUrl,
                            fixture.leagueName ?: fixture.competitionName,
                            fixture.leagueId,
                            onTeamSelect,
                            modifier = Modifier.weight(1f),
                        )
                        if (fixture.homeScore != null && fixture.awayScore != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(
                                    alpha = 0.88f,
                                ),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                Text(
                                    text = stringResource(
                                        Res.string.match_score,
                                        fixture.homeScore,
                                        fixture.awayScore,
                                    ),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(
                                        horizontal = 14.dp,
                                        vertical = 6.dp,
                                    ),
                                )
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                Text(
                                    text = "VS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp,
                                    ),
                                )
                            }
                        }
                        MatchTeam(
                            fixture.awayTeamId,
                            fixture.awayTeamName,
                            teamNames.away,
                            fixture.awayTeamLogoUrl,
                            fixture.leagueName ?: fixture.competitionName,
                            fixture.leagueId,
                            onTeamSelect,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    fixture.competitionName?.let { competition ->
                        Text(
                            text = stringResource(Res.string.fixture_competition, competition),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        item {
            MatchDetailSection(stringResource(Res.string.match_kick_off)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            formatFixtureDay(fixture.kickoffEpochMillis),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            formatFixtureDate(fixture.kickoffEpochMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(
                            text = formatFixtureTime(fixture.kickoffEpochMillis),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
        if (fixture.venueName != null || fixture.venueAddress != null) {
            item {
                MatchDetailSection(stringResource(Res.string.match_venue)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            fixture.venueName?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            fixture.venueAddress
                                ?.takeIf { it != fixture.venueName }
                                ?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                        }
                        GoogleMapPreview(
                            latitude = fixture.latitude,
                            longitude = fixture.longitude,
                            address = location,
                            onNavigate = {
                                locationLauncher.navigate(
                                    fixture.latitude,
                                    fixture.longitude,
                                    location,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                        Button(
                            onClick = {
                                locationLauncher.navigate(
                                    fixture.latitude,
                                    fixture.longitude,
                                    location,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            MaterialSymbolIcon(
                                symbol = MaterialSymbol.LocationOn,
                                contentDescription = null,
                                size = 18.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(Res.string.match_directions),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        item {
            MatchWeatherSection(
                weather = weather,
                isLoading = isWeatherLoading,
                unavailable = weatherUnavailable,
            )
        }
    }
}

@Composable
private fun RefereeAppointmentSection(
    fixture: Fixture,
    allocationUpdatingId: String?,
    allocationFailure: RefreshFailure?,
    onRequestDecision: (PendingAllocationDecision) -> Unit,
) {
    val roleLabel = if (fixture.refereeRole != null) {
        stringResource(Res.string.fixture_role_referee_named, fixture.refereeRole)
    } else {
        stringResource(Res.string.fixture_role_referee)
    }
    MatchDetailSection(stringResource(Res.string.schedule_allocation_section_title)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MaterialSymbolIcon(
                        symbol = MaterialSymbol.Sports,
                        contentDescription = null,
                        size = 20.dp,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = roleLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                RefereeStatusPill(fixture = fixture)
            }
            if (allocationFailure != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.schedule_allocation_update_failed),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            if (fixture.hasAssignedRefereeAllocation()) {
                RefereeAllocationActions(
                    updating = allocationUpdatingId == fixture.refereeAllocationId,
                    actionsEnabled = allocationUpdatingId == null,
                    showAccept = fixture.canAcceptRefereeAllocation(),
                    showDecline = fixture.canDeclineRefereeAllocation(),
                    onAccept = {
                        onRequestDecision(
                            PendingAllocationDecision(
                                fixture = fixture,
                                decision = RefereeAllocationDecision.Accept,
                            ),
                        )
                    },
                    onDecline = {
                        onRequestDecision(
                            PendingAllocationDecision(
                                fixture = fixture,
                                decision = RefereeAllocationDecision.Decline,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MatchWeatherSection(
    weather: MatchWeather?,
    isLoading: Boolean,
    unavailable: Boolean,
) {
    MatchDetailSection(stringResource(Res.string.match_weather)) {
        when {
            isLoading -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.match_weather_loading))
                }
            }

            weather != null -> {
                val temperatureUnit = platformWeatherTemperatureUnit()
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        weather.condition?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(
                                alpha = 0.8f,
                            ),
                        ) {
                            Text(
                                text = stringResource(
                                    Res.string.match_weather_temperature,
                                    temperatureUnit
                                        .fromCelsius(weather.minimumTemperatureCelsius)
                                        .roundToInt()
                                        .toString(),
                                    temperatureUnit
                                        .fromCelsius(weather.maximumTemperatureCelsius)
                                        .roundToInt()
                                        .toString(),
                                    temperatureUnit.symbol,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Text(
                        stringResource(
                            Res.string.match_weather_rain,
                            "${weather.rainfallProbabilityPercent}%",
                            weather.rainfallAmountMillimetres.toOneDecimalString(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(
                                Res.string.match_weather_humidity,
                                "${weather.relativeHumidityPercent}%",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (weather.uvIndex > 0) {
                            Text(
                                stringResource(Res.string.match_weather_uv, weather.uvIndex),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (weather.sunscreenRecommended) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(Res.string.match_weather_sunscreen),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                    if (weather.hasElevatedWashoutRisk) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(Res.string.match_weather_washout),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }

            unavailable -> Text(
                stringResource(Res.string.match_weather_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Double.toOneDecimalString(): String {
    val tenths = (this * 10).roundToInt()
    val absoluteTenths = abs(tenths)
    val sign = if (tenths < 0) "-" else ""
    return "$sign${absoluteTenths / TENTHS_PER_UNIT}.${absoluteTenths % TENTHS_PER_UNIT}"
}

private const val TENTHS_PER_UNIT = 10

@Composable
private fun MatchDetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    SportsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun fixtureStatusLabel(
    status: FixtureStatus,
): String = stringResource(
    when (status) {
        FixtureStatus.Pending -> Res.string.match_status_pending
        FixtureStatus.Scheduled -> Res.string.match_status_scheduled
        FixtureStatus.Live -> Res.string.match_status_live
        FixtureStatus.Completed -> Res.string.match_status_completed
        FixtureStatus.Postponed -> Res.string.match_status_postponed
        FixtureStatus.Washout -> Res.string.match_status_washout
        FixtureStatus.Cancelled -> Res.string.match_status_cancelled
        FixtureStatus.Unknown -> Res.string.match_status_unknown
    },
)

@Composable
private fun MatchTeam(
    teamId: String?,
    name: String,
    displayName: String,
    logoUrl: String?,
    competitionName: String?,
    leagueId: String?,
    onSelect: (String, String, String?, String?, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .then(
                if (teamId == null) {
                    Modifier
                } else {
                    Modifier.clickable {
                        onSelect(teamId, name, logoUrl, competitionName, leagueId)
                    }
                },
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RemoteImage(
            url = logoUrl,
            contentDescription = stringResource(Res.string.content_team_logo, name),
            fallbackIcon = MaterialSymbol.Groups,
            modifier = Modifier.size(64.dp),
        )
        Text(
            displayName,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(
    message: String,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message)
    }
}

@Composable
private fun LoadFailure(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text(stringResource(Res.string.action_retry)) }
    }
}
