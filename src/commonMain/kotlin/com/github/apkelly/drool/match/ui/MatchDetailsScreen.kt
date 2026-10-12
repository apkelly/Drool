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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import com.github.apkelly.drool.common.domain.model.MatchWeather
import com.github.apkelly.drool.common.domain.model.RefreshFailure
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_retry
import com.github.apkelly.drool.common.resources.content_team_logo
import com.github.apkelly.drool.common.resources.fixture_competition
import com.github.apkelly.drool.common.resources.match_details_title
import com.github.apkelly.drool.common.resources.match_directions
import com.github.apkelly.drool.common.resources.match_kick_off
import com.github.apkelly.drool.common.resources.match_load_failed
import com.github.apkelly.drool.common.resources.match_round
import com.github.apkelly.drool.common.resources.match_round_unknown
import com.github.apkelly.drool.common.resources.match_score
import com.github.apkelly.drool.common.resources.match_status
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
import com.github.apkelly.drool.common.ui.format.displayTeamNames
import com.github.apkelly.drool.common.ui.format.formatFixtureDate
import com.github.apkelly.drool.common.ui.format.formatFixtureDay
import com.github.apkelly.drool.common.ui.format.formatFixtureTime
import com.github.apkelly.drool.common.ui.platform.GoogleMapPreview
import com.github.apkelly.drool.common.ui.platform.rememberLocationActionLauncher
import com.github.apkelly.drool.common.ui.widgets.CacheStatus
import com.github.apkelly.drool.common.ui.widgets.MaterialBackIcon
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.common.ui.widgets.RemoteImage
import com.github.apkelly.drool.match.ui.model.MatchDetailsUiState
import com.github.apkelly.drool.match.ui.platform.platformWeatherTemperatureUnit
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailsScreen(
    profileId: String,
    matchId: String,
    state: MatchDetailsUiState,
    onBack: () -> Unit,
    onLoad: (String, String) -> Unit,
    onRefresh: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    onTeamSelect: (String, String, String?, String?, String?) -> Unit,
) {
    val currentOnLoad by rememberUpdatedState(onLoad)
    LaunchedEffect(profileId, matchId) { currentOnLoad(profileId, matchId) }
    val details = (state as? MatchDetailsUiState.Content)
        ?.takeIf { it.profileId == profileId && it.matchId == matchId }
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
        PullToRefreshBox(
            isRefreshing = details?.isRefreshing == true,
            onRefresh = { onRefresh(profileId, matchId) },
            modifier = Modifier.fillMaxSize().padding(padding),
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

                else -> MatchDetails(
                    fixture = details.fixture,
                    weather = details.weather,
                    isWeatherLoading = details.isWeatherLoading,
                    weatherUnavailable = details.weatherUnavailable,
                    refreshFailed = details.refreshFailed,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    onRefresh = { onRefresh(profileId, matchId) },
                    onTeamSelect = onTeamSelect,
                )
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
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onRefresh: () -> Unit,
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
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
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
                    teamNames.away,
                    fixture.awayTeamLogoUrl,
                    fixture.leagueName ?: fixture.competitionName,
                    fixture.leagueId,
                    onTeamSelect,
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
                val temperatureUnit = platformWeatherTemperatureUnit()
                weather.condition?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    stringResource(
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
                )
                Text(
                    stringResource(
                        Res.string.match_weather_humidity,
                        "${weather.relativeHumidityPercent}%",
                    ),
                )
                Text(
                    stringResource(
                        Res.string.match_weather_rain,
                        "${weather.rainfallProbabilityPercent}%",
                        weather.rainfallAmountMillimetres.toOneDecimalString(),
                    ),
                )
                if (weather.uvIndex > 0) {
                    Text(stringResource(Res.string.match_weather_uv, weather.uvIndex))
                }
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
    return "$sign${absoluteTenths / TENTHS_PER_UNIT}.${absoluteTenths % TENTHS_PER_UNIT}"
}

private const val TENTHS_PER_UNIT = 10

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
