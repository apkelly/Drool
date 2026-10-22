@file:Suppress("MatchingDeclarationName")

package com.github.apkelly.drool.common.ui.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.RefereeAllocationDecision
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_cancel
import com.github.apkelly.drool.common.resources.schedule_allocation_accept
import com.github.apkelly.drool.common.resources.schedule_allocation_accept_body
import com.github.apkelly.drool.common.resources.schedule_allocation_accept_title
import com.github.apkelly.drool.common.resources.schedule_allocation_decline
import com.github.apkelly.drool.common.resources.schedule_allocation_decline_body
import com.github.apkelly.drool.common.resources.schedule_allocation_decline_title
import com.github.apkelly.drool.common.resources.schedule_allocation_status_accepted
import com.github.apkelly.drool.common.resources.schedule_allocation_status_contact_requested
import com.github.apkelly.drool.common.resources.schedule_allocation_status_declined
import com.github.apkelly.drool.common.resources.schedule_allocation_status_pending
import com.github.apkelly.drool.common.ui.format.displayTeamNames
import org.jetbrains.compose.resources.stringResource

data class PendingAllocationDecision(
    val fixture: Fixture,
    val decision: RefereeAllocationDecision,
)

@Composable
fun RefereeAllocationConfirmationDialog(
    pending: PendingAllocationDecision?,
    onDismiss: () -> Unit,
    onConfirm: (Fixture, RefereeAllocationDecision) -> Unit,
) {
    val item = pending ?: return
    val teamNames = item.fixture.displayTeamNames()
    val isAccept = item.decision == RefereeAllocationDecision.Accept
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isAccept) {
                        Res.string.schedule_allocation_accept_title
                    } else {
                        Res.string.schedule_allocation_decline_title
                    },
                ),
            )
        },
        text = {
            Text(
                if (isAccept) {
                    stringResource(
                        Res.string.schedule_allocation_accept_body,
                        teamNames.home,
                        teamNames.away,
                    )
                } else {
                    stringResource(Res.string.schedule_allocation_decline_body)
                },
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onConfirm(item.fixture, item.decision)
                },
                colors = if (isAccept) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                },
            ) {
                Text(
                    stringResource(
                        if (isAccept) {
                            Res.string.schedule_allocation_accept
                        } else {
                            Res.string.schedule_allocation_decline
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
fun RefereeStatusPill(
    fixture: Fixture,
    modifier: Modifier = Modifier,
) {
    if (fixture.role != FixtureRole.Referee) return
    if (fixture.refereeStatus.isNullOrBlank() && !fixture.hasAssignedRefereeAllocation()) return
    val status = fixture.refereeStatus?.lowercase()
    val isAccepted = status == "accepted"
    val isContactRequested = status == "request_contact"
    val isDeclined = status == "declined" || status == "rejected"
    val label = stringResource(
        when {
            isAccepted -> Res.string.schedule_allocation_status_accepted
            isContactRequested -> Res.string.schedule_allocation_status_contact_requested
            isDeclined -> Res.string.schedule_allocation_status_declined
            else -> Res.string.schedule_allocation_status_pending
        },
    )
    val containerColor = when {
        isAccepted -> MaterialTheme.colorScheme.primaryContainer
        isContactRequested || isDeclined -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
    }
    val contentColor = when {
        isAccepted -> MaterialTheme.colorScheme.onPrimaryContainer
        isContactRequested || isDeclined -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun RefereeAllocationActions(
    updating: Boolean,
    actionsEnabled: Boolean,
    showAccept: Boolean,
    showDecline: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (updating) {
        Row(
            modifier = modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
        }
        return
    }
    if (!showAccept && !showDecline) return

    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
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
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(
                    text = stringResource(Res.string.schedule_allocation_decline),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

internal fun Fixture.hasAssignedRefereeAllocation(): Boolean = role == FixtureRole.Referee &&
    !refereeAllocationId.isNullOrBlank()

internal fun Fixture.canAcceptRefereeAllocation(): Boolean = hasAssignedRefereeAllocation() &&
    refereeStatus?.lowercase() != "accepted"

internal fun Fixture.canDeclineRefereeAllocation(): Boolean = hasAssignedRefereeAllocation() &&
    refereeStatus?.lowercase() !in CLOSED_REFEREE_STATUSES

internal fun Fixture.mergeRefereeAllocationFrom(
    other: Fixture?,
): Fixture {
    if (other == null) return this
    return copy(
        role = if (other.role == FixtureRole.Referee) FixtureRole.Referee else role,
        refereeRole = other.refereeRole ?: refereeRole,
        refereeAllocationId = other.refereeAllocationId ?: refereeAllocationId,
        refereeStatus = other.refereeStatus ?: refereeStatus,
    )
}

private val CLOSED_REFEREE_STATUSES =
    setOf("request_contact", "declined", "rejected")
