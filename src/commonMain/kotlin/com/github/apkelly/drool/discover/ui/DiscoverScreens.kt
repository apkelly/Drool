package com.github.apkelly.drool.discover.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.Team
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import com.github.apkelly.drool.common.domain.model.ThemeMode
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_back
import com.github.apkelly.drool.common.resources.club_no_teams
import com.github.apkelly.drool.common.resources.club_teams
import com.github.apkelly.drool.common.resources.discover_empty
import com.github.apkelly.drool.common.resources.discover_search
import com.github.apkelly.drool.common.resources.discover_title
import com.github.apkelly.drool.common.ui.model.CollectionUiState
import com.github.apkelly.drool.common.ui.preview.TopLevelScreenPreviewData
import com.github.apkelly.drool.common.ui.theme.DroolTheme
import com.github.apkelly.drool.common.ui.widgets.CacheStatus
import com.github.apkelly.drool.common.ui.widgets.ClubCard
import com.github.apkelly.drool.common.ui.widgets.MaterialBackIcon
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbolIcon
import com.github.apkelly.drool.common.ui.widgets.RefreshAction
import com.github.apkelly.drool.common.ui.widgets.TeamCard
import com.github.apkelly.drool.discover.ui.model.DiscoverUiModel
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.lazy.items as lazyItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    model: DiscoverUiModel,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onClubSelect: (Club) -> Unit,
) {
    val state = model.clubs
    var query by remember { mutableStateOf("") }
    val filtered = remember(state.items, query) {
        state.items.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }
    androidx.compose.material3.Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.discover_title)) },
                actions = { RefreshAction(state.isRefreshing, onRefresh) },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(280.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(Res.string.discover_search)) },
                        leadingIcon = {
                            MaterialSymbolIcon(
                                MaterialSymbol.Search,
                                contentDescription = null,
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    CacheStatus(
                        isStale = state.isStale,
                        failure = state.refreshFailure,
                        hasContent = state.items.isNotEmpty(),
                        onRetry = onRefresh,
                    )
                }
                if (filtered.isEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(Res.string.discover_empty),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { club ->
                        ClubCard(club = club, onClick = { onClubSelect(club) })
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Discover",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
private fun DiscoverScreenPreview() {
    DroolTheme(mode = ThemeMode.Light) {
        DiscoverScreen(
            model = TopLevelScreenPreviewData.discover,
            onRefresh = {},
            onClubSelect = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubScreen(
    club: Club,
    teams: CollectionUiState<Team>,
    relationships: Map<String, TeamRelationship>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onFollowingChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onTeamSelect: (Team) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(club.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MaterialBackIcon(
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                actions = { RefreshAction(teams.isRefreshing, onRefresh) },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = teams.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ClubCard(club = club, onClick = {})
                }
                item {
                    Text(
                        stringResource(Res.string.club_teams),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                item {
                    CacheStatus(
                        isStale = teams.isStale,
                        failure = teams.refreshFailure,
                        hasContent = teams.items.isNotEmpty(),
                        onRetry = onRefresh,
                    )
                }
                if (teams.items.isEmpty()) {
                    item { Text(stringResource(Res.string.club_no_teams)) }
                } else {
                    lazyItems(teams.items, key = { it.id }) { team ->
                        TeamCard(
                            team = team,
                            relationship = relationships[team.id] ?: TeamRelationship.None,
                            onFollowingChange = { following ->
                                onFollowingChange(team.id, following)
                            },
                            onClick = { onTeamSelect(team) },
                        )
                    }
                }
            }
        }
    }
}
