package com.github.apkelly.drool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FamilyProfile
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.schedule_empty
import com.github.apkelly.drool.resources.schedule_empty_body
import com.github.apkelly.drool.resources.schedule_title
import com.github.apkelly.drool.ui.model.CollectionUiState
import com.github.apkelly.drool.ui.widgets.CacheStatus
import com.github.apkelly.drool.ui.widgets.FixtureCard
import com.github.apkelly.drool.ui.widgets.FamilyProfileSelector
import com.github.apkelly.drool.ui.widgets.RefreshAction
import com.github.apkelly.drool.ui.format.formatFixtureDateHeading
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    state: CollectionUiState<Fixture>,
    familyProfiles: List<FamilyProfile>,
    selectedProfileId: String?,
    onProfileSelected: (String?) -> Unit,
    onRefresh: () -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.schedule_title)) },
                actions = { RefreshAction(state.isRefreshing, onRefresh) },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    FamilyProfileSelector(
                        profiles = familyProfiles,
                        selectedProfileId = selectedProfileId,
                        onSelected = onProfileSelected,
                    )
                }
                item {
                    CacheStatus(
                        isStale = state.isStale,
                        failure = state.refreshFailure,
                        hasContent = state.items.isNotEmpty(),
                        onRetry = onRefresh,
                    )
                }
                if (state.items.isEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.schedule_empty),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(stringResource(Res.string.schedule_empty_body))
                    }
                } else {
                    state.items
                        .groupBy { formatFixtureDateHeading(it.kickoffEpochMillis) }
                        .forEach { (dateHeading, fixtures) ->
                            item(key = "date-$dateHeading") {
                                Text(
                                    text = dateHeading,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            items(
                                fixtures,
                                key = { "${it.profileId}:${it.id}" },
                            ) { fixture ->
                                FixtureCard(
                                    fixture = fixture,
                                    profileName = if (selectedProfileId == null) {
                                        familyProfiles
                                            .firstOrNull { it.id == fixture.profileId }
                                            ?.displayName
                                    } else {
                                        null
                                    },
                                )
                        }
                    }
                }
            }
        }
    }
}
