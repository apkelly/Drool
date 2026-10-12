package com.github.apkelly.drool.match.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.MatchSheetOfficial
import com.github.apkelly.drool.common.domain.model.MatchSheetTeam
import com.github.apkelly.drool.common.domain.model.TeamSheetStatus
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_retry
import com.github.apkelly.drool.common.resources.match_sheet_away_team
import com.github.apkelly.drool.common.resources.match_sheet_confirmed
import com.github.apkelly.drool.common.resources.match_sheet_home_team
import com.github.apkelly.drool.common.resources.match_sheet_load_failed
import com.github.apkelly.drool.common.resources.match_sheet_officials
import com.github.apkelly.drool.common.resources.match_sheet_open
import com.github.apkelly.drool.common.resources.match_sheet_referee
import com.github.apkelly.drool.common.resources.match_sheet_submitted
import com.github.apkelly.drool.match.ui.model.MatchSheetUiState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MatchSheetSummary(
    profileId: String,
    matchId: String,
    state: MatchSheetUiState,
    onRetry: () -> Unit,
    onTeamSelect: (String) -> Unit,
) {
    when (val content = state as? MatchSheetUiState.Content) {
        null -> {
            if (
                state is MatchSheetUiState.Failed &&
                state.profileId == profileId &&
                state.matchId == matchId
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(Res.string.match_sheet_load_failed))
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        else -> {
            if (content.profileId != profileId || content.matchId != matchId) return
            val sheet = content.matchSheet
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    MatchSheetTeamRow(
                        label = stringResource(Res.string.match_sheet_home_team),
                        team = sheet.homeTeam,
                        onClick = { onTeamSelect(sheet.homeTeam.id) },
                    )
                }
                item { HorizontalDivider() }
                item {
                    MatchSheetTeamRow(
                        label = stringResource(Res.string.match_sheet_away_team),
                        team = sheet.awayTeam,
                        onClick = { onTeamSelect(sheet.awayTeam.id) },
                    )
                }
                item { HorizontalDivider() }
                item {
                    OfficialsSection(
                        title = stringResource(Res.string.match_sheet_referee),
                        officials = sheet.referees,
                    )
                }
                item {
                    OfficialsSection(
                        title = stringResource(Res.string.match_sheet_officials),
                        officials = sheet.officials,
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchSheetTeamRow(
    label: String,
    team: MatchSheetTeam,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(team.name, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = stringResource(
                when (team.status) {
                    TeamSheetStatus.Open -> Res.string.match_sheet_open
                    TeamSheetStatus.Submitted -> Res.string.match_sheet_submitted
                    TeamSheetStatus.Confirmed -> Res.string.match_sheet_confirmed
                },
            ),
            color = when (team.status) {
                TeamSheetStatus.Open -> MaterialTheme.colorScheme.onSurfaceVariant
                TeamSheetStatus.Submitted -> MaterialTheme.colorScheme.primary
                TeamSheetStatus.Confirmed -> MaterialTheme.colorScheme.tertiary
            },
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OfficialsSection(
    title: String,
    officials: List<MatchSheetOfficial>,
) {
    if (officials.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        officials.forEach { official ->
            Text(
                listOfNotNull(
                    official.displayName.takeIf(String::isNotBlank),
                    official.roleName?.takeIf(String::isNotBlank),
                ).joinToString(" — "),
            )
        }
    }
}
