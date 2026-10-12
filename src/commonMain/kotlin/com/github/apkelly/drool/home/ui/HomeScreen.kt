package com.github.apkelly.drool.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.common.domain.model.ProfileTeam
import com.github.apkelly.drool.common.domain.model.RefreshFailure
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.domain.model.ThemeMode
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_sign_in
import com.github.apkelly.drool.common.resources.home_greeting
import com.github.apkelly.drool.common.resources.home_no_games
import com.github.apkelly.drool.common.resources.home_no_games_body
import com.github.apkelly.drool.common.resources.home_overview
import com.github.apkelly.drool.common.resources.home_upcoming
import com.github.apkelly.drool.common.resources.home_view_schedule
import com.github.apkelly.drool.common.resources.linked_profile_refresh_failed
import com.github.apkelly.drool.common.resources.linked_profile_session_expired
import com.github.apkelly.drool.common.resources.linked_profile_teams
import com.github.apkelly.drool.common.ui.preview.TopLevelScreenPreviewData
import com.github.apkelly.drool.common.ui.theme.DroolTheme
import com.github.apkelly.drool.common.ui.widgets.CacheStatus
import com.github.apkelly.drool.common.ui.widgets.FixtureCard
import com.github.apkelly.drool.common.ui.widgets.LinkedProfileSelector
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbolIcon
import com.github.apkelly.drool.common.ui.widgets.ProfileTeamCard
import com.github.apkelly.drool.home.ui.model.HomeUiModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    model: HomeUiModel,
    onProfileSelect: (String?) -> Unit,
    onRefresh: () -> Unit,
    onSignInAgain: () -> Unit,
    onFixtureSelect: (Fixture) -> Unit,
    onViewSchedule: () -> Unit,
    modifier: Modifier = Modifier,
    onTeamSelect: (ProfileTeam) -> Unit,
) {
    val profile = model.profile
    val linkedProfiles = model.linkedProfiles
    val selectedProfileId = model.selectedProfileId
    val profileTeams = model.profileTeams
    val linkedProfileRefreshResults = model.linkedProfileRefreshResults
    val fixtures = model.fixtures
    val expiredProfileNames = linkedProfileRefreshResults
        .filterValues {
            it is RefreshResult.Failed &&
                (
                    it.reason == RefreshFailure.Unauthorized ||
                        it.reason == RefreshFailure.Forbidden
                    )
        }
        .keys
        .map { profileId ->
            linkedProfiles.firstOrNull { it.id == profileId }?.displayName ?: profileId
        }
        .distinct()
        .joinToString()

    PullToRefreshBox(
        isRefreshing = fixtures.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableWidth = maxWidth - HOME_GRID_PADDING * 2
            val compactLayout = maxWidth < TABLET_BREAKPOINT
            val columnCount = (
                (availableWidth + HOME_GRID_SPACING) /
                    (HOME_TEAM_CARD_MIN_WIDTH + HOME_GRID_SPACING)
                ).toInt().coerceAtLeast(HOME_TEAM_MIN_COLUMNS)

            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(HOME_GRID_PADDING),
                horizontalArrangement = Arrangement.spacedBy(HOME_GRID_SPACING),
                verticalArrangement = Arrangement.spacedBy(HOME_GRID_SPACING),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(Res.string.home_greeting, profile.displayName),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(Res.string.home_overview),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LinkedProfileSelector(
                        profiles = linkedProfiles,
                        selectedProfileId = selectedProfileId,
                        onSelect = onProfileSelect,
                    )
                }
                if (expiredProfileNames.isNotEmpty()) {
                    item(
                        key = "expired-linked-profile-session",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(
                                    Res.string.linked_profile_session_expired,
                                    expiredProfileNames,
                                ),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(onClick = onSignInAgain) {
                                Text(stringResource(Res.string.action_sign_in))
                            }
                        }
                    }
                }
                linkedProfileRefreshResults.forEach { (profileId, result) ->
                    if (
                        result is RefreshResult.Failed &&
                        result.reason != RefreshFailure.Unauthorized &&
                        result.reason != RefreshFailure.Forbidden
                    ) {
                        item(
                            key = "refresh-failure-$profileId",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Text(
                                text = stringResource(
                                    Res.string.linked_profile_refresh_failed,
                                    linkedProfiles.firstOrNull { it.id == profileId }
                                        ?.displayName
                                        ?: profileId,
                                ),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CacheStatus(
                        isStale = fixtures.isStale,
                        failure = fixtures.refreshFailure,
                        hasContent = fixtures.items.isNotEmpty(),
                        onRetry = onRefresh,
                    )
                }
                val now = Clock.System.now().toEpochMilliseconds()
                val upcoming = fixtures.items
                    .filter { it.kickoffEpochMillis >= now }
                    .upcomingOnNextDay()
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(Res.string.home_upcoming),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onViewSchedule) {
                            MaterialSymbolIcon(
                                MaterialSymbol.CalendarMonth,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.home_view_schedule))
                        }
                    }
                }
                if (upcoming.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(Res.string.home_no_games),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(stringResource(Res.string.home_no_games_body))
                        }
                    }
                } else {
                    items(
                        items = upcoming,
                        key = { "upcoming:${it.profileId}:${it.id}:${it.role}" },
                        span = { GridItemSpan(maxLineSpan) },
                    ) { fixture ->
                        FixtureCard(
                            fixture = fixture,
                            profileName = linkedProfiles.profileNameFor(
                                fixture.profileId,
                                selectedProfileId,
                            ),
                            stackedTeams = true,
                            onClick = { onFixtureSelect(fixture) },
                        )
                    }
                }

                if (profileTeams.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(Res.string.linked_profile_teams),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    items(
                        key = { "${it.profileId}:${it.team.id}" },
                        items = profileTeams,
                    ) { profileTeam ->
                        ProfileTeamCard(
                            profileTeam = profileTeam,
                            profileName = linkedProfiles
                                .firstOrNull { it.id == profileTeam.profileId }
                                ?.displayName
                                ?: profileTeam.profileId,
                            modifier = if (compactLayout) {
                                Modifier.fillMaxWidth()
                            } else {
                                Modifier.aspectRatio(1f)
                            },
                            compact = compactLayout,
                            onClick = { onTeamSelect(profileTeam) },
                        )
                    }
                }
            }
        }
    }
}

internal fun List<Fixture>.upcomingOnNextDay(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): List<Fixture> {
    val nextDate = minOfOrNull {
        Instant.fromEpochMilliseconds(it.kickoffEpochMillis)
            .toLocalDateTime(timeZone)
            .date
    } ?: return emptyList()
    return filter {
        Instant.fromEpochMilliseconds(it.kickoffEpochMillis)
            .toLocalDateTime(timeZone)
            .date == nextDate
    }.sortedBy(Fixture::kickoffEpochMillis)
}

private val HOME_TEAM_CARD_MIN_WIDTH = 240.dp
private const val HOME_TEAM_MIN_COLUMNS = 1
private val TABLET_BREAKPOINT = 600.dp
private val HOME_GRID_PADDING = 20.dp
private val HOME_GRID_SPACING = 14.dp

private fun List<LinkedProfile>.profileNameFor(
    profileId: String?,
    selectedProfileId: String?,
): String? = if (selectedProfileId == null) {
    firstOrNull { it.id == profileId }?.displayName
} else {
    null
}

@Preview(
    name = "Home",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
private fun HomeScreenPreview() {
    DroolTheme(mode = ThemeMode.Light) {
        HomeScreen(
            model = TopLevelScreenPreviewData.home,
            onProfileSelect = {},
            onRefresh = {},
            onSignInAgain = {},
            onFixtureSelect = {},
            onViewSchedule = {},
            onTeamSelect = {},
        )
    }
}
