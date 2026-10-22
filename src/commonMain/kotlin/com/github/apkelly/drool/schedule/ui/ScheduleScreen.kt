package com.github.apkelly.drool.schedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.RefereeAllocationDecision
import com.github.apkelly.drool.common.domain.model.ThemeMode
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.availability_action
import com.github.apkelly.drool.common.resources.schedule_allocation_update_failed
import com.github.apkelly.drool.common.resources.schedule_description
import com.github.apkelly.drool.common.resources.schedule_empty
import com.github.apkelly.drool.common.resources.schedule_empty_body
import com.github.apkelly.drool.common.resources.schedule_title
import com.github.apkelly.drool.common.ui.format.formatFixtureDateHeading
import com.github.apkelly.drool.common.ui.preview.TopLevelScreenPreviewData
import com.github.apkelly.drool.common.ui.theme.DroolTheme
import com.github.apkelly.drool.common.ui.widgets.CacheStatus
import com.github.apkelly.drool.common.ui.widgets.FixtureCard
import com.github.apkelly.drool.common.ui.widgets.LinkedProfileSelector
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbolIcon
import com.github.apkelly.drool.common.ui.widgets.PendingAllocationDecision
import com.github.apkelly.drool.common.ui.widgets.RefereeAllocationActions
import com.github.apkelly.drool.common.ui.widgets.RefereeAllocationConfirmationDialog
import com.github.apkelly.drool.common.ui.widgets.RefreshAction
import com.github.apkelly.drool.common.ui.widgets.SportsCard
import com.github.apkelly.drool.common.ui.widgets.canAcceptRefereeAllocation
import com.github.apkelly.drool.common.ui.widgets.canDeclineRefereeAllocation
import com.github.apkelly.drool.common.ui.widgets.hasAssignedRefereeAllocation
import com.github.apkelly.drool.schedule.ui.model.ScheduleUiModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    model: ScheduleUiModel,
    onProfileSelect: (String?) -> Unit,
    onRefresh: () -> Unit,
    onFixtureSelect: (Fixture) -> Unit,
    onAllocationDecision: (Fixture, RefereeAllocationDecision) -> Unit,
    modifier: Modifier = Modifier,
    onOpenAvailability: () -> Unit,
) {
    val state = model.fixtures
    val linkedProfiles = model.linkedProfiles
    val selectedProfileId = model.selectedProfileId
    var pendingDecision by remember { mutableStateOf<PendingAllocationDecision?>(null) }

    RefereeAllocationConfirmationDialog(
        pending = pendingDecision,
        onDismiss = { pendingDecision = null },
        onConfirm = onAllocationDecision,
    )

    androidx.compose.material3.Scaffold(
        modifier = modifier,
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
                    Text(
                        stringResource(Res.string.schedule_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    LinkedProfileSelector(
                        profiles = linkedProfiles,
                        selectedProfileId = selectedProfileId,
                        onSelect = onProfileSelect,
                    )
                }
                if (model.canManageRefereeAvailability) {
                    item {
                        OutlinedButton(onClick = onOpenAvailability) {
                            MaterialSymbolIcon(
                                symbol = MaterialSymbol.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(stringResource(Res.string.availability_action))
                        }
                    }
                }
                item {
                    CacheStatus(
                        isStale = state.isStale,
                        failure = state.refreshFailure,
                        hasContent = state.items.isNotEmpty(),
                        onRetry = onRefresh,
                    )
                }
                if (model.allocationFailure != null) {
                    item {
                        SportsCard(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Text(
                                text = stringResource(
                                    Res.string.schedule_allocation_update_failed,
                                ),
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                if (state.items.isEmpty()) {
                    item {
                        SportsCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MaterialSymbolIcon(
                                    symbol = MaterialSymbol.CalendarMonth,
                                    contentDescription = null,
                                    size = 36.dp,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    stringResource(Res.string.schedule_empty),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    stringResource(Res.string.schedule_empty_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    state.items
                        .groupBy { formatFixtureDateHeading(it.kickoffEpochMillis) }
                        .forEach { (dateHeading, fixtures) ->
                            item(key = "date-$dateHeading") {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(top = 6.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 14.dp,
                                            vertical = 6.dp,
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        MaterialSymbolIcon(
                                            symbol = MaterialSymbol.CalendarMonth,
                                            contentDescription = null,
                                            size = 18.dp,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = dateHeading,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                            items(
                                fixtures,
                                key = { "${it.profileId}:${it.id}:${it.role}" },
                            ) { fixture ->
                                FixtureCard(
                                    fixture = fixture,
                                    profileName = if (selectedProfileId == null) {
                                        linkedProfiles
                                            .firstOrNull { it.id == fixture.profileId }
                                            ?.displayName
                                    } else {
                                        null
                                    },
                                    onClick = { onFixtureSelect(fixture) },
                                    footer = if (fixture.hasAssignedRefereeAllocation()) {
                                        {
                                            RefereeAllocationActions(
                                                updating = model.allocationUpdatingId ==
                                                    fixture.refereeAllocationId,
                                                actionsEnabled =
                                                model.allocationUpdatingId == null,
                                                showAccept =
                                                fixture.canAcceptRefereeAllocation(),
                                                showDecline =
                                                fixture.canDeclineRefereeAllocation(),
                                                onAccept = {
                                                    pendingDecision = PendingAllocationDecision(
                                                        fixture = fixture,
                                                        decision = RefereeAllocationDecision.Accept,
                                                    )
                                                },
                                                onDecline = {
                                                    pendingDecision = PendingAllocationDecision(
                                                        fixture = fixture,
                                                        decision =
                                                        RefereeAllocationDecision.Decline,
                                                    )
                                                },
                                            )
                                        }
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

@Preview(
    name = "Schedule",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
private fun ScheduleScreenPreview() {
    DroolTheme(mode = ThemeMode.Light) {
        ScheduleScreen(
            model = TopLevelScreenPreviewData.schedule,
            onProfileSelect = {},
            onRefresh = {},
            onFixtureSelect = {},
            onAllocationDecision = { _, _ -> },
            onOpenAvailability = {},
        )
    }
}
