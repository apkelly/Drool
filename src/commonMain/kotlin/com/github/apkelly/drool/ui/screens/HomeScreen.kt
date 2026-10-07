package com.github.apkelly.drool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FamilyProfile
import com.github.apkelly.drool.domain.model.FamilyTeam
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.RefreshResult
import com.github.apkelly.drool.domain.model.RefreshFailure
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.home_greeting
import com.github.apkelly.drool.resources.home_next_game
import com.github.apkelly.drool.resources.home_no_games
import com.github.apkelly.drool.resources.home_no_games_body
import com.github.apkelly.drool.resources.home_overview
import com.github.apkelly.drool.resources.home_view_schedule
import com.github.apkelly.drool.resources.family_teams
import com.github.apkelly.drool.resources.family_profile_refresh_failed
import com.github.apkelly.drool.resources.family_profile_session_expired
import com.github.apkelly.drool.resources.action_sign_in
import com.github.apkelly.drool.ui.model.CollectionUiState
import com.github.apkelly.drool.ui.widgets.CacheStatus
import com.github.apkelly.drool.ui.widgets.FixtureCard
import com.github.apkelly.drool.ui.widgets.FamilyProfileSelector
import com.github.apkelly.drool.ui.widgets.FamilyTeamCard
import com.github.apkelly.drool.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.ui.widgets.MaterialSymbolIcon
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profile: Profile,
    familyProfiles: List<FamilyProfile>,
    selectedProfileId: String?,
    familyTeams: List<FamilyTeam>,
    familyRefreshResults: Map<String, RefreshResult>,
    onProfileSelected: (String?) -> Unit,
    fixtures: CollectionUiState<Fixture>,
    onRefresh: () -> Unit,
    onSignInAgain: () -> Unit,
    onFixtureSelected: (Fixture) -> Unit,
    onViewSchedule: () -> Unit,
    onTeamSelected: (FamilyTeam) -> Unit,
) {
    val expiredProfileNames = familyRefreshResults
        .filterValues {
            it is RefreshResult.Failed &&
                (it.reason == RefreshFailure.Unauthorized ||
                    it.reason == RefreshFailure.Forbidden)
        }
        .keys
        .map { profileId ->
            familyProfiles.firstOrNull { it.id == profileId }?.displayName ?: profileId
        }
        .distinct()
        .joinToString()

    PullToRefreshBox(
        isRefreshing = fixtures.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableWidth = maxWidth - HOME_GRID_PADDING * 2
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
                FamilyProfileSelector(
                    profiles = familyProfiles,
                    selectedProfileId = selectedProfileId,
                    onSelected = onProfileSelected,
                )
            }
            if (expiredProfileNames.isNotEmpty()) {
                item(
                    key = "expired-family-session",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(
                                Res.string.family_profile_session_expired,
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
            familyRefreshResults.forEach { (profileId, result) ->
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
                                Res.string.family_profile_refresh_failed,
                                familyProfiles.firstOrNull { it.id == profileId }
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
            val next = fixtures.items.firstOrNull()
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Res.string.home_next_game),
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
            if (next == null) {
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
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FixtureCard(
                        fixture = next,
                        profileName = familyProfiles.profileNameFor(
                            next.profileId,
                            selectedProfileId,
                        ),
                        onClick = { onFixtureSelected(next) },
                    )
                }
            }
            if (familyTeams.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(Res.string.family_teams),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                items(
                    key = { "${it.profileId}:${it.team.id}" },
                    items = familyTeams,
                ) { familyTeam ->
                    FamilyTeamCard(
                        familyTeam = familyTeam,
                        profileName = familyProfiles
                            .firstOrNull { it.id == familyTeam.profileId }
                            ?.displayName
                            ?: familyTeam.profileId,
                        modifier = Modifier.aspectRatio(1f),
                        onClick = { onTeamSelected(familyTeam) },
                    )
                }
            }
            }
        }
    }
}

private val HOME_TEAM_CARD_MIN_WIDTH = 240.dp
private const val HOME_TEAM_MIN_COLUMNS = 2
private val HOME_GRID_PADDING = 20.dp
private val HOME_GRID_SPACING = 14.dp

private fun List<FamilyProfile>.profileNameFor(
    profileId: String?,
    selectedProfileId: String?,
): String? = if (selectedProfileId == null) {
    firstOrNull { it.id == profileId }?.displayName
} else {
    null
}
