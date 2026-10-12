package com.github.apkelly.drool.schedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.RefereeAllocationDecision
import com.github.apkelly.drool.common.domain.model.ThemeMode
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_cancel
import com.github.apkelly.drool.common.resources.availability_action
import com.github.apkelly.drool.common.resources.schedule_allocation_accept
import com.github.apkelly.drool.common.resources.schedule_allocation_decline
import com.github.apkelly.drool.common.resources.schedule_allocation_decline_body
import com.github.apkelly.drool.common.resources.schedule_allocation_decline_title
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
import com.github.apkelly.drool.common.ui.widgets.RefreshAction
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
    var pendingDecline by remember { mutableStateOf<Fixture?>(null) }

    pendingDecline?.let { fixture ->
        AlertDialog(
            onDismissRequest = { pendingDecline = null },
            title = { Text(stringResource(Res.string.schedule_allocation_decline_title)) },
            text = { Text(stringResource(Res.string.schedule_allocation_decline_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDecline = null
                        onAllocationDecision(fixture, RefereeAllocationDecision.Decline)
                    },
                ) {
                    Text(stringResource(Res.string.schedule_allocation_decline))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDecline = null }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

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
                        Text(
                            text = stringResource(
                                Res.string.schedule_allocation_update_failed,
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
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
                                                    onAllocationDecision(
                                                        fixture,
                                                        RefereeAllocationDecision.Accept,
                                                    )
                                                },
                                                onDecline = { pendingDecline = fixture },
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

@Composable
private fun RefereeAllocationActions(
    updating: Boolean,
    actionsEnabled: Boolean,
    showAccept: Boolean,
    showDecline: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    if (updating) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showAccept) {
            Button(
                onClick = onAccept,
                enabled = actionsEnabled,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.schedule_allocation_accept))
            }
        }
        if (showDecline) {
            OutlinedButton(
                onClick = onDecline,
                enabled = actionsEnabled,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.schedule_allocation_decline))
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

internal fun Fixture.hasAssignedRefereeAllocation(): Boolean = role == FixtureRole.Referee &&
    !refereeAllocationId.isNullOrBlank()

internal fun Fixture.canAcceptRefereeAllocation(): Boolean = hasAssignedRefereeAllocation() &&
    refereeStatus?.lowercase() != "accepted"

internal fun Fixture.canDeclineRefereeAllocation(): Boolean = hasAssignedRefereeAllocation() &&
    refereeStatus?.lowercase() !in CLOSED_REFEREE_STATUSES

private val CLOSED_REFEREE_STATUSES =
    setOf("request_contact", "declined", "rejected")
