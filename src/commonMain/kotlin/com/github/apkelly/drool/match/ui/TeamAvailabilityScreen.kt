package com.github.apkelly.drool.match.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.apkelly.drool.common.domain.model.MatchSheetMember
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.content_profile_image
import com.github.apkelly.drool.common.resources.match_sheet_confirm
import com.github.apkelly.drool.common.resources.match_sheet_confirmed
import com.github.apkelly.drool.common.resources.match_sheet_member_update_failed
import com.github.apkelly.drool.common.resources.match_sheet_no_members
import com.github.apkelly.drool.common.resources.match_sheet_open
import com.github.apkelly.drool.common.resources.match_sheet_submit
import com.github.apkelly.drool.common.resources.match_sheet_submitted
import com.github.apkelly.drool.common.resources.match_sheet_unconfirm
import com.github.apkelly.drool.common.resources.match_sheet_unsubmit
import com.github.apkelly.drool.common.resources.team_availability_title
import com.github.apkelly.drool.common.ui.widgets.MaterialBackIcon
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.common.ui.widgets.RemoteImage
import com.github.apkelly.drool.match.ui.model.MatchSheetUiState
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamAvailabilityScreen(
    teamId: String,
    state: MatchSheetUiState,
    canEditMembers: Boolean,
    teamAction: TeamSheetAction?,
    onBack: () -> Unit,
    onAvailabilityChange: (String, Boolean) -> Unit,
    onTeamAction: (TeamSheetAction) -> Unit,
    modifier: Modifier = Modifier,
    memberCard: @Composable (MatchSheetMember, Modifier) -> Unit = { _, _ -> },
) {
    val content = state as? MatchSheetUiState.Content
    val sheet = content?.matchSheet
    val team = sheet?.homeTeam?.takeIf { it.id == teamId }
        ?: sheet?.awayTeam?.takeIf { it.id == teamId }
    val members = sheet?.members.orEmpty().filter { it.teamId == teamId }
    var selectedMember by remember { mutableStateOf<MatchSheetMember?>(null) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState(),
    )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        team?.name ?: stringResource(Res.string.team_availability_title),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MaterialBackIcon(contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        when {
            content == null || team == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (teamAction != null) {
                    item {
                        Button(
                            onClick = { onTeamAction(teamAction) },
                            enabled = content.updatingTeamId == null,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        ) {
                            if (content.updatingTeamId == teamId) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(teamActionLabel(teamAction))
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(team.status.label()),
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                if (content.updateFailed) {
                    item {
                        Text(
                            stringResource(Res.string.match_sheet_member_update_failed),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (members.isEmpty()) {
                    item { Text(stringResource(Res.string.match_sheet_no_members)) }
                } else {
                    members
                        .groupBy { it.roleName.takeIf(String::isNotBlank) ?: "Team" }
                        .forEach { (role, roleMembers) ->
                            item {
                                Text(
                                    role,
                                    modifier = Modifier.padding(top = 12.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            items(roleMembers, key = MatchSheetMember::id) { member ->
                                MemberRow(
                                    member = member,
                                    canEdit = canEditMembers && team.status ==
                                        com.github.apkelly.drool.common.domain.model.TeamSheetStatus.Open,
                                    updating = content.updatingMemberId == member.id,
                                    onSelect = { selectedMember = member },
                                    onAvailabilityChange = {
                                        onAvailabilityChange(member.id, it)
                                    },
                                )
                            }
                        }
                }
            }
        }
    }

    selectedMember?.let { member ->
        Dialog(
            onDismissRequest = { selectedMember = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val cardInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { selectedMember = null },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                memberCard(
                    member,
                    Modifier
                        .fillMaxWidth(MATCH_SHEET_CARD_WIDTH_FRACTION)
                        .widthIn(max = 360.dp)
                        .clickable(
                            indication = null,
                            interactionSource = cardInteractionSource,
                            onClick = {},
                        ),
                )
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: MatchSheetMember,
    canEdit: Boolean,
    updating: Boolean,
    onSelect: () -> Unit,
    onAvailabilityChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RemoteImage(
            url = member.imageUrl,
            contentDescription = stringResource(
                Res.string.content_profile_image,
                member.displayName,
            ),
            fallbackIcon = MaterialSymbol.Person,
            modifier = Modifier.size(48.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(member.displayName, style = MaterialTheme.typography.titleSmall)
            member.jerseyNumber?.let { Text("#$it") }
        }
        if (updating) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Checkbox(
                checked = member.available,
                enabled = canEdit,
                onCheckedChange = if (canEdit) onAvailabilityChange else null,
            )
        }
    }
}

@Composable
private fun teamActionLabel(
    action: TeamSheetAction,
) = stringResource(
    when (action) {
        TeamSheetAction.Submit -> Res.string.match_sheet_submit
        TeamSheetAction.Unsubmit -> Res.string.match_sheet_unsubmit
        TeamSheetAction.Confirm -> Res.string.match_sheet_confirm
        TeamSheetAction.Unconfirm -> Res.string.match_sheet_unconfirm
    },
)

private fun com.github.apkelly.drool.common.domain.model.TeamSheetStatus.label() = when (this) {
    com.github.apkelly.drool.common.domain.model.TeamSheetStatus.Open ->
        Res.string.match_sheet_open

    com.github.apkelly.drool.common.domain.model.TeamSheetStatus.Submitted ->
        Res.string.match_sheet_submitted

    com.github.apkelly.drool.common.domain.model.TeamSheetStatus.Confirmed ->
        Res.string.match_sheet_confirmed
}

private const val MATCH_SHEET_CARD_WIDTH_FRACTION = 0.92f
