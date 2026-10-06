package com.github.apkelly.drool.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FixtureStatus
import com.github.apkelly.drool.domain.model.LadderEntry
import com.github.apkelly.drool.domain.model.MatchWeather
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.action_retry
import com.github.apkelly.drool.resources.content_team_logo
import com.github.apkelly.drool.resources.fixture_competition
import com.github.apkelly.drool.resources.ladder_drawn
import com.github.apkelly.drool.resources.ladder_goal_difference
import com.github.apkelly.drool.resources.ladder_lost
import com.github.apkelly.drool.resources.ladder_played
import com.github.apkelly.drool.resources.ladder_points
import com.github.apkelly.drool.resources.ladder_won
import com.github.apkelly.drool.resources.match_details_title
import com.github.apkelly.drool.resources.match_directions
import com.github.apkelly.drool.resources.match_kick_off
import com.github.apkelly.drool.resources.match_load_failed
import com.github.apkelly.drool.resources.match_round
import com.github.apkelly.drool.resources.match_round_unknown
import com.github.apkelly.drool.resources.match_score
import com.github.apkelly.drool.resources.match_status
import com.github.apkelly.drool.resources.match_status_cancelled
import com.github.apkelly.drool.resources.match_status_completed
import com.github.apkelly.drool.resources.match_status_live
import com.github.apkelly.drool.resources.match_status_pending
import com.github.apkelly.drool.resources.match_status_postponed
import com.github.apkelly.drool.resources.match_status_scheduled
import com.github.apkelly.drool.resources.match_status_unknown
import com.github.apkelly.drool.resources.match_status_washout
import com.github.apkelly.drool.resources.match_venue
import com.github.apkelly.drool.resources.match_weather
import com.github.apkelly.drool.resources.match_weather_humidity
import com.github.apkelly.drool.resources.match_weather_loading
import com.github.apkelly.drool.resources.match_weather_rain
import com.github.apkelly.drool.resources.match_weather_sunscreen
import com.github.apkelly.drool.resources.match_weather_temperature
import com.github.apkelly.drool.resources.match_weather_unavailable
import com.github.apkelly.drool.resources.match_weather_uv
import com.github.apkelly.drool.resources.match_weather_washout
import com.github.apkelly.drool.resources.team_ladders
import com.github.apkelly.drool.resources.team_load_failed
import com.github.apkelly.drool.resources.team_matches
import com.github.apkelly.drool.resources.team_no_ladder
import com.github.apkelly.drool.resources.team_no_matches
import com.github.apkelly.drool.resources.team_no_results
import com.github.apkelly.drool.resources.team_results
import com.github.apkelly.drool.ui.format.formatFixtureDateTime
import com.github.apkelly.drool.ui.format.formatFixtureDate
import com.github.apkelly.drool.ui.format.formatFixtureDay
import com.github.apkelly.drool.ui.format.formatFixtureTime
import com.github.apkelly.drool.ui.model.MatchDetailsUiState
import com.github.apkelly.drool.ui.model.TeamHubUiState
import com.github.apkelly.drool.ui.platform.GoogleMapPreview
import com.github.apkelly.drool.ui.platform.rememberLocationActionLauncher
import com.github.apkelly.drool.ui.widgets.RemoteImage
import com.github.apkelly.drool.ui.widgets.CacheStatus
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamHubScreen(
    profileId: String,
    teamId: String,
    teamName: String,
    logoUrl: String?,
    primaryColor: String?,
    secondaryColor: String?,
    state: TeamHubUiState,
    onBack: () -> Unit,
    onLoad: (String, String) -> Unit,
    onRefresh: (String, String) -> Unit,
    onMatchSelected: (Fixture) -> Unit,
) {
    LaunchedEffect(profileId, teamId) { onLoad(profileId, teamId) }
    val brand = teamColor(primaryColor, MaterialTheme.colorScheme.primary)
    val accent = teamColor(secondaryColor, MaterialTheme.colorScheme.secondary)
    val headerContentColor = if (brand.luminance() > 0.5f) Color.Black else Color.White
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState()
    )
    var selectedTab by remember(teamId) { mutableIntStateOf(0) }
    val content = (state as? TeamHubUiState.Content)
        ?.takeIf { it.profileId == profileId && it.teamId == teamId }
    val current = content?.hub

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    TeamToolbarTitle(
                        teamName = teamName,
                        logoUrl = logoUrl,
                        secondaryColor = accent,
                        expanded = scrollBehavior.state.collapsedFraction < 0.5f,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = brand,
                    scrolledContainerColor = brand,
                    navigationIconContentColor = headerContentColor,
                    titleContentColor = headerContentColor,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = content?.isRefreshing == true,
            onRefresh = { onRefresh(profileId, teamId) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    listOf(
                        Res.string.team_matches,
                        Res.string.team_results,
                        Res.string.team_ladders,
                    ).forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(stringResource(label)) },
                        )
                    }
                }
                if (content != null) {
                    CacheStatus(
                        isStale = content.isStale,
                        failure = content.refreshFailure,
                        hasContent = true,
                        onRetry = { onRefresh(profileId, teamId) },
                    )
                }
                when {
                    state is TeamHubUiState.Failed &&
                        state.profileId == profileId && state.teamId == teamId ->
                        LoadFailure(
                            message = stringResource(Res.string.team_load_failed),
                            onRetry = { onLoad(profileId, teamId) },
                        )
                    current == null -> LoadingContent()
                    selectedTab == 0 -> FixtureList(
                        fixtures = current.matches,
                        emptyMessage = stringResource(Res.string.team_no_matches),
                        onSelected = onMatchSelected,
                    )
                    selectedTab == 1 -> FixtureList(
                        fixtures = current.results,
                        emptyMessage = stringResource(Res.string.team_no_results),
                        onSelected = onMatchSelected,
                    )
                    else -> LadderList(
                        name = current.ladderName,
                        entries = current.ladder,
                        highlightedTeamId = teamId,
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamToolbarTitle(
    teamName: String,
    logoUrl: String?,
    secondaryColor: Color,
    expanded: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (expanded) {
            RemoteImage(
                url = logoUrl,
                contentDescription = stringResource(Res.string.content_team_logo, teamName),
                fallbackIcon = Icons.Default.Groups,
                modifier = Modifier.size(64.dp),
            )
        }
        Column {
            Text(
                teamName,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
            )
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                Spacer(
                    Modifier.width(80.dp).height(4.dp).background(secondaryColor)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailsScreen(
    profileId: String,
    matchId: String,
    state: MatchDetailsUiState,
    onBack: () -> Unit,
    onLoad: (String, String) -> Unit,
    onTeamSelected: (String, String, String?) -> Unit,
) {
    LaunchedEffect(profileId, matchId) { onLoad(profileId, matchId) }
    val details = (state as? MatchDetailsUiState.Content)
        ?.takeIf { it.profileId == profileId && it.matchId == matchId }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.match_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        when {
            state is MatchDetailsUiState.Failed &&
                state.profileId == profileId && state.matchId == matchId -> {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    LoadFailure(
                        message = stringResource(Res.string.match_load_failed),
                        onRetry = { onLoad(profileId, matchId) },
                    )
                }
            }
            details == null -> {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    LoadingContent()
                }
            }
            else -> MatchDetails(
                fixture = details.fixture,
                weather = details.weather,
                isWeatherLoading = details.isWeatherLoading,
                weatherUnavailable = details.weatherUnavailable,
                contentPadding = padding,
                onTeamSelected = onTeamSelected,
            )
        }
    }
}

@Composable
private fun FixtureList(
    fixtures: List<Fixture>,
    emptyMessage: String,
    onSelected: (Fixture) -> Unit,
) {
    if (fixtures.isEmpty()) {
        EmptyContent(emptyMessage)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(fixtures, key = { it.id }) { fixture ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelected(fixture) },
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        formatFixtureDateTime(fixture.kickoffEpochMillis),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        "${fixture.homeTeamName} vs ${fixture.awayTeamName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (fixture.homeScore != null && fixture.awayScore != null) {
                        Text(
                            stringResource(
                                Res.string.match_score,
                                fixture.homeScore,
                                fixture.awayScore,
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    fixture.competitionName?.let {
                        Text(stringResource(Res.string.fixture_competition, it))
                    }
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
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onTeamSelected: (String, String, String?) -> Unit,
) {
    val locationLauncher = rememberLocationActionLauncher()
    val location = fixture.venueAddress ?: fixture.venueName.orEmpty()
    val round = fixture.roundLabel?.let {
        if (it.startsWith("round", ignoreCase = true)) it
        else stringResource(Res.string.match_round, it)
    } ?: stringResource(Res.string.match_round_unknown)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                MatchTeam(
                    fixture.homeTeamId,
                    fixture.homeTeamName,
                    fixture.homeTeamLogoUrl,
                    onTeamSelected,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (fixture.homeScore != null && fixture.awayScore != null) {
                        stringResource(
                            Res.string.match_score,
                            fixture.homeScore,
                            fixture.awayScore,
                        )
                    } else {
                        "vs"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                MatchTeam(
                    fixture.awayTeamId,
                    fixture.awayTeamName,
                    fixture.awayTeamLogoUrl,
                    onTeamSelected,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Text(round, style = MaterialTheme.typography.headlineSmall)
        }
        item {
            MatchDetailSection(stringResource(Res.string.match_kick_off)) {
                Text(
                    formatFixtureDay(fixture.kickoffEpochMillis),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(formatFixtureDate(fixture.kickoffEpochMillis))
                Text(formatFixtureTime(fixture.kickoffEpochMillis))
            }
        }
        item {
            MatchDetailSection(stringResource(Res.string.match_status)) {
                Text(
                    fixtureStatusLabel(fixture.status),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        item {
            MatchWeatherSection(
                weather = weather,
                isLoading = isWeatherLoading,
                unavailable = weatherUnavailable,
            )
        }
        if (fixture.venueName != null || fixture.venueAddress != null) {
            item {
                MatchDetailSection(stringResource(Res.string.match_venue)) {
                    fixture.venueName?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium)
                    }
                    fixture.venueAddress
                        ?.takeIf { it != fixture.venueName }
                        ?.let { Text(it) }
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
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Text(
                        stringResource(Res.string.match_directions),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        fixture.competitionName?.let {
            item { Text(stringResource(Res.string.fixture_competition, it)) }
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
                weather.condition?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    stringResource(
                        Res.string.match_weather_temperature,
                        weather.minimumTemperatureCelsius.roundToInt().toString(),
                        weather.maximumTemperatureCelsius.roundToInt().toString(),
                    )
                )
                Text(
                    stringResource(
                        Res.string.match_weather_humidity,
                        "${weather.relativeHumidityPercent}%",
                    )
                )
                Text(
                    stringResource(
                        Res.string.match_weather_rain,
                        "${weather.rainfallProbabilityPercent}%",
                        weather.rainfallAmountMillimetres.toOneDecimalString(),
                    )
                )
                Text(stringResource(Res.string.match_weather_uv, weather.uvIndex))
                if (weather.sunscreenRecommended) {
                    Text(
                        stringResource(Res.string.match_weather_sunscreen),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (weather.hasElevatedWashoutRisk) {
                    Text(
                        stringResource(Res.string.match_weather_washout),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            unavailable -> Text(stringResource(Res.string.match_weather_unavailable))
        }
    }
}

private fun Double.toOneDecimalString(): String {
    val tenths = (this * 10).roundToInt()
    val absoluteTenths = abs(tenths)
    val sign = if (tenths < 0) "-" else ""
    return "$sign${absoluteTenths / 10}.${absoluteTenths % 10}"
}

@Composable
private fun MatchDetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun fixtureStatusLabel(status: FixtureStatus): String =
    stringResource(
        when (status) {
            FixtureStatus.Pending -> Res.string.match_status_pending
            FixtureStatus.Scheduled -> Res.string.match_status_scheduled
            FixtureStatus.Live -> Res.string.match_status_live
            FixtureStatus.Completed -> Res.string.match_status_completed
            FixtureStatus.Postponed -> Res.string.match_status_postponed
            FixtureStatus.Washout -> Res.string.match_status_washout
            FixtureStatus.Cancelled -> Res.string.match_status_cancelled
            FixtureStatus.Unknown -> Res.string.match_status_unknown
        }
    )

@Composable
private fun MatchTeam(
    teamId: String?,
    name: String,
    logoUrl: String?,
    onSelected: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .then(
                if (teamId == null) Modifier
                else Modifier.clickable { onSelected(teamId, name, logoUrl) }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RemoteImage(
            url = logoUrl,
            contentDescription = stringResource(Res.string.content_team_logo, name),
            fallbackIcon = Icons.Default.Groups,
            modifier = Modifier.size(64.dp),
        )
        Text(
            name,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LadderList(
    name: String?,
    entries: List<LadderEntry>,
    highlightedTeamId: String,
) {
    if (entries.isEmpty()) {
        EmptyContent(stringResource(Res.string.team_no_ladder))
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        name?.let {
            item {
                Text(
                    it,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        item { LadderHeader() }
        items(entries, key = { "${it.position}:${it.teamId}:${it.teamName}" }) { entry ->
            LadderRow(entry, entry.teamId == highlightedTeamId)
        }
    }
}

@Composable
private fun LadderHeader() {
    Row(Modifier.fillMaxWidth().padding(8.dp)) {
        Text("#", modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
        Text("", modifier = Modifier.weight(1f))
        listOf(
            Res.string.ladder_played,
            Res.string.ladder_won,
            Res.string.ladder_drawn,
            Res.string.ladder_lost,
            Res.string.ladder_goal_difference,
            Res.string.ladder_points,
        ).forEach {
            Text(
                stringResource(it),
                modifier = Modifier.width(34.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun LadderRow(entry: LadderEntry, highlighted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (highlighted) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            (entry.position ?: 0).toString(),
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End,
        )
        Spacer(Modifier.width(10.dp))
        RemoteImage(
            url = entry.logoUrl,
            contentDescription = entry.teamName,
            fallbackIcon = Icons.Default.Groups,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(entry.teamName, modifier = Modifier.weight(1f))
        listOf(
            entry.played,
            entry.won,
            entry.drawn,
            entry.lost,
            entry.goalDifference,
            entry.points,
        ).forEach {
            Text(
                (it ?: 0).toString(),
                modifier = Modifier.width(34.dp),
                textAlign = TextAlign.End,
            )
        }
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
private fun EmptyContent(message: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message)
    }
}

@Composable
private fun LoadFailure(message: String, onRetry: () -> Unit) {
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

private fun teamColor(value: String?, fallback: Color): Color {
    val hex = value?.trim()?.removePrefix("#") ?: return fallback
    val parsed = hex.toLongOrNull(16) ?: return fallback
    return when (hex.length) {
        6 -> Color((0xFF000000L or parsed).toInt())
        8 -> Color(parsed.toInt())
        else -> fallback
    }
}
