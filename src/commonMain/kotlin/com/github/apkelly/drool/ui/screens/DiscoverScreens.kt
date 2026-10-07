package com.github.apkelly.drool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
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
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.domain.model.Club
import com.github.apkelly.drool.domain.model.Team
import com.github.apkelly.drool.domain.model.TeamRelationship
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.action_back
import com.github.apkelly.drool.resources.club_no_teams
import com.github.apkelly.drool.resources.club_teams
import com.github.apkelly.drool.resources.discover_empty
import com.github.apkelly.drool.resources.discover_search
import com.github.apkelly.drool.resources.discover_title
import com.github.apkelly.drool.ui.model.CollectionUiState
import com.github.apkelly.drool.ui.widgets.CacheStatus
import com.github.apkelly.drool.ui.widgets.ClubCard
import com.github.apkelly.drool.ui.widgets.RefreshAction
import com.github.apkelly.drool.ui.widgets.TeamCard
import com.github.apkelly.drool.ui.widgets.MaterialBackIcon
import com.github.apkelly.drool.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.ui.widgets.MaterialSymbolIcon
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    state: CollectionUiState<Club>,
    onRefresh: () -> Unit,
    onClubSelected: (Club) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(state.items, query) {
        state.items.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }
    androidx.compose.material3.Scaffold(
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
                        ClubCard(club = club, onClick = { onClubSelected(club) })
                    }
                }
            }
        }
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
    onFollowingChanged: (String, Boolean) -> Unit,
    onTeamSelected: (Team) -> Unit,
) {
    androidx.compose.material3.Scaffold(
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
                            onFollowingChanged = { following ->
                                onFollowingChanged(team.id, following)
                            },
                            onClick = { onTeamSelected(team) },
                        )
                    }
                }
            }
        }
    }
}
