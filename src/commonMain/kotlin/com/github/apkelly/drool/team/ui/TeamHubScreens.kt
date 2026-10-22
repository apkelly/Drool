package com.github.apkelly.drool.team.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.LadderEntry
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_retry
import com.github.apkelly.drool.common.resources.content_team_logo
import com.github.apkelly.drool.common.resources.fixture_competition
import com.github.apkelly.drool.common.resources.ladder_drawn
import com.github.apkelly.drool.common.resources.ladder_goal_difference
import com.github.apkelly.drool.common.resources.ladder_lost
import com.github.apkelly.drool.common.resources.ladder_played
import com.github.apkelly.drool.common.resources.ladder_points
import com.github.apkelly.drool.common.resources.ladder_won
import com.github.apkelly.drool.common.resources.match_score
import com.github.apkelly.drool.common.resources.team_ladders
import com.github.apkelly.drool.common.resources.team_load_failed
import com.github.apkelly.drool.common.resources.team_matches
import com.github.apkelly.drool.common.resources.team_no_ladder
import com.github.apkelly.drool.common.resources.team_no_matches
import com.github.apkelly.drool.common.resources.team_no_results
import com.github.apkelly.drool.common.resources.team_results
import com.github.apkelly.drool.common.ui.format.displayTeamNames
import com.github.apkelly.drool.common.ui.format.formatFixtureDateTime
import com.github.apkelly.drool.common.ui.widgets.CacheStatus
import com.github.apkelly.drool.common.ui.widgets.MaterialBackIcon
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.common.ui.widgets.RemoteImage
import com.github.apkelly.drool.common.ui.widgets.SportsCard
import com.github.apkelly.drool.team.ui.model.TeamHubUiState
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
    modifier: Modifier = Modifier,
    onMatchSelect: (Fixture) -> Unit,
) {
    val currentOnLoad by rememberUpdatedState(onLoad)
    LaunchedEffect(profileId, teamId) { currentOnLoad(profileId, teamId) }
    val brand = teamColor(primaryColor, MaterialTheme.colorScheme.primary)
    val accent = teamColor(secondaryColor, MaterialTheme.colorScheme.secondary)
    val headerContentColor = if (brand.luminance() > 0.5f) Color.Black else Color.White
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState(),
    )
    var selectedTab by remember(teamId) { mutableIntStateOf(0) }
    val content = (state as? TeamHubUiState.Content)
        ?.takeIf { it.profileId == profileId && it.teamId == teamId }
    val current = content?.hub

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                        MaterialBackIcon(contentDescription = null)
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
                        onSelect = onMatchSelect,
                    )

                    selectedTab == 1 -> FixtureList(
                        fixtures = current.results,
                        emptyMessage = stringResource(Res.string.team_no_results),
                        onSelect = onMatchSelect,
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
                fallbackIcon = MaterialSymbol.Groups,
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
                    Modifier.width(80.dp).height(4.dp).background(secondaryColor),
                )
            }
        }
    }
}

@Composable
private fun FixtureList(
    fixtures: List<Fixture>,
    emptyMessage: String,
    onSelect: (Fixture) -> Unit,
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
            val teamNames = fixture.displayTeamNames()
            SportsCard(
                onClick = { onSelect(fixture) },
                modifier = Modifier.fillMaxWidth(),
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
                        "${teamNames.home} vs ${teamNames.away}",
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
private fun LadderRow(
    entry: LadderEntry,
    highlighted: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (highlighted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
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
            fallbackIcon = MaterialSymbol.Groups,
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

private fun teamColor(
    value: String?,
    fallback: Color,
): Color {
    val hex = value?.trim()?.removePrefix("#") ?: return fallback
    val parsed = hex.toLongOrNull(16) ?: return fallback
    return when (hex.length) {
        RGB_HEX_LENGTH -> Color((OPAQUE_ALPHA_MASK or parsed).toInt())
        ARGB_HEX_LENGTH -> Color(parsed.toInt())
        else -> fallback
    }
}

private const val RGB_HEX_LENGTH = 6
private const val ARGB_HEX_LENGTH = 8
private const val OPAQUE_ALPHA_MASK = 0xFF000000L
