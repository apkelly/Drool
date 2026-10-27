package com.github.apkelly.drool.match.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import com.github.apkelly.drool.common.domain.model.MatchHalf
import com.github.apkelly.drool.common.domain.model.MatchSheet
import com.github.apkelly.drool.common.domain.model.MatchSheetMember
import com.github.apkelly.drool.common.domain.model.TeamSheetStatus
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_cancel
import com.github.apkelly.drool.common.resources.action_confirm
import com.github.apkelly.drool.common.resources.action_retry
import com.github.apkelly.drool.common.resources.match_score
import com.github.apkelly.drool.common.resources.match_sheet_confirmed
import com.github.apkelly.drool.common.resources.match_sheet_load_failed
import com.github.apkelly.drool.common.resources.match_sheet_open
import com.github.apkelly.drool.common.resources.match_sheet_submitted
import com.github.apkelly.drool.common.resources.referee_checklist_checkin_action
import com.github.apkelly.drool.common.resources.referee_checklist_checkin_pending
import com.github.apkelly.drool.common.resources.referee_checklist_checkin_ready
import com.github.apkelly.drool.common.resources.referee_checklist_checkin_title
import com.github.apkelly.drool.common.resources.referee_checklist_opposition_confirmed_pending
import com.github.apkelly.drool.common.resources.referee_checklist_opposition_confirmed_ready
import com.github.apkelly.drool.common.resources.referee_checklist_opposition_confirmed_title
import com.github.apkelly.drool.common.resources.referee_checklist_section_title
import com.github.apkelly.drool.common.resources.referee_checklist_status_done
import com.github.apkelly.drool.common.resources.referee_checklist_status_pending
import com.github.apkelly.drool.common.resources.referee_checklist_teams_entered_body
import com.github.apkelly.drool.common.resources.referee_checklist_teams_entered_title
import com.github.apkelly.drool.common.resources.referee_goal_dialog_kids_title
import com.github.apkelly.drool.common.resources.referee_goal_dialog_own_goal
import com.github.apkelly.drool.common.resources.referee_goal_dialog_select_player
import com.github.apkelly.drool.common.resources.referee_goal_dialog_select_team
import com.github.apkelly.drool.common.resources.referee_goal_dialog_subtitle
import com.github.apkelly.drool.common.resources.referee_goal_dialog_title
import com.github.apkelly.drool.common.resources.referee_goals_add_goal
import com.github.apkelly.drool.common.resources.referee_goals_adult_hint
import com.github.apkelly.drool.common.resources.referee_goals_empty
import com.github.apkelly.drool.common.resources.referee_goals_half_first_short
import com.github.apkelly.drool.common.resources.referee_goals_half_second_short
import com.github.apkelly.drool.common.resources.referee_goals_half_time_score
import com.github.apkelly.drool.common.resources.referee_goals_kids_hint
import com.github.apkelly.drool.common.resources.referee_goals_minute
import com.github.apkelly.drool.common.resources.referee_goals_own_goal_badge
import com.github.apkelly.drool.common.resources.referee_goals_player_numbered
import com.github.apkelly.drool.common.resources.referee_goals_remove
import com.github.apkelly.drool.common.resources.referee_goals_section_title
import com.github.apkelly.drool.common.resources.referee_goals_team_goal
import com.github.apkelly.drool.common.resources.referee_submit_adult_body
import com.github.apkelly.drool.common.resources.referee_submit_failed
import com.github.apkelly.drool.common.resources.referee_submit_score_and_goals_button
import com.github.apkelly.drool.common.resources.referee_submit_score_body
import com.github.apkelly.drool.common.resources.referee_submit_score_button
import com.github.apkelly.drool.common.resources.referee_submit_section_title
import com.github.apkelly.drool.common.resources.referee_submit_success
import com.github.apkelly.drool.common.resources.referee_timer_end_first_half
import com.github.apkelly.drool.common.resources.referee_timer_end_match
import com.github.apkelly.drool.common.resources.referee_timer_pause
import com.github.apkelly.drool.common.resources.referee_timer_phase_first_half
import com.github.apkelly.drool.common.resources.referee_timer_phase_full_time
import com.github.apkelly.drool.common.resources.referee_timer_phase_half_time
import com.github.apkelly.drool.common.resources.referee_timer_phase_not_started
import com.github.apkelly.drool.common.resources.referee_timer_phase_second_half
import com.github.apkelly.drool.common.resources.referee_timer_preset_adult
import com.github.apkelly.drool.common.resources.referee_timer_preset_label
import com.github.apkelly.drool.common.resources.referee_timer_preset_u18
import com.github.apkelly.drool.common.resources.referee_timer_preset_u8
import com.github.apkelly.drool.common.resources.referee_timer_reset
import com.github.apkelly.drool.common.resources.referee_timer_reset_confirm_body
import com.github.apkelly.drool.common.resources.referee_timer_reset_confirm_title
import com.github.apkelly.drool.common.resources.referee_timer_resume
import com.github.apkelly.drool.common.resources.referee_timer_section_title
import com.github.apkelly.drool.common.resources.referee_timer_start
import com.github.apkelly.drool.common.resources.referee_timer_start_second_half
import com.github.apkelly.drool.common.resources.referee_timer_stoppage
import com.github.apkelly.drool.common.resources.referee_timer_target
import com.github.apkelly.drool.common.ui.format.displayTeamNames
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbolIcon
import com.github.apkelly.drool.common.ui.widgets.SportsCard
import com.github.apkelly.drool.common.ui.widgets.toColorOrNull
import com.github.apkelly.drool.match.domain.model.RecordedRefereeGoal
import com.github.apkelly.drool.match.domain.model.RefereeMatchPreset
import com.github.apkelly.drool.match.domain.model.RefereeMatchSession
import com.github.apkelly.drool.match.domain.model.RefereeTimerPhase
import com.github.apkelly.drool.match.ui.model.MatchSheetUiState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

internal fun shouldShowRefereeChecklistTab(
    fixture: Fixture?,
    nowEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
): Boolean {
    if (fixture == null) return false
    if (fixture.role != FixtureRole.Referee) return false
    if (fixture.matchSheetId.isNullOrBlank()) return false
    if (
        fixture.status == FixtureStatus.Cancelled ||
        fixture.status == FixtureStatus.Washout ||
        fixture.status == FixtureStatus.Postponed
    ) {
        return false
    }
    val windowStart = fixture.kickoffEpochMillis - REFEREE_CHECKLIST_LEAD_MILLIS
    val windowEnd = fixture.kickoffEpochMillis + REFEREE_CHECKLIST_TRAIL_MILLIS
    return nowEpochMillis in windowStart..windowEnd
}

@Composable
@Suppress("LongParameterList")
internal fun RefereeChecklistTab(
    profileId: String,
    matchId: String,
    fixture: Fixture,
    state: MatchSheetUiState,
    onRetry: () -> Unit,
    onCheckInReferee: (String) -> Unit,
    onSelectPreset: (RefereeMatchPreset) -> Unit,
    onToggleTimer: (Long) -> Unit,
    onAdvancePhase: (Long) -> Unit,
    onResetTimer: () -> Unit,
    onRecordGoal: (Boolean, MatchSheetMember?, Boolean, Long) -> Unit,
    onRemoveGoal: (String) -> Unit,
    onSubmitReport: (Long) -> Unit,
    onToggleGoalOwnGoal: (String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(20.dp),
    initialAdultGoalDialogForHome: Boolean? = null,
    initialAdultGoalOwnGoal: Boolean = false,
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
            RefereeChecklistContent(
                profileId = profileId,
                fixture = fixture,
                content = content,
                contentPadding = contentPadding,
                onCheckInReferee = onCheckInReferee,
                onSelectPreset = onSelectPreset,
                onToggleTimer = onToggleTimer,
                onAdvancePhase = onAdvancePhase,
                onResetTimer = onResetTimer,
                onRecordGoal = onRecordGoal,
                onRemoveGoal = onRemoveGoal,
                onToggleGoalOwnGoal = onToggleGoalOwnGoal,
                onSubmitReport = onSubmitReport,
                initialAdultGoalDialogForHome = initialAdultGoalDialogForHome,
                initialAdultGoalOwnGoal = initialAdultGoalOwnGoal,
            )
        }
    }
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun RefereeChecklistContent(
    profileId: String,
    fixture: Fixture,
    content: MatchSheetUiState.Content,
    contentPadding: PaddingValues,
    onCheckInReferee: (String) -> Unit,
    onSelectPreset: (RefereeMatchPreset) -> Unit,
    onToggleTimer: (Long) -> Unit,
    onAdvancePhase: (Long) -> Unit,
    onResetTimer: () -> Unit,
    onRecordGoal: (Boolean, MatchSheetMember?, Boolean, Long) -> Unit,
    onRemoveGoal: (String) -> Unit,
    onToggleGoalOwnGoal: (String) -> Unit,
    onSubmitReport: (Long) -> Unit,
    initialAdultGoalDialogForHome: Boolean? = null,
    initialAdultGoalOwnGoal: Boolean = false,
) {
    val session = content.refereeSession
    var nowEpochMillis by remember {
        mutableLongStateOf(Clock.System.now().toEpochMilliseconds())
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_START ||
                event == Lifecycle.Event.ON_RESUME
            ) {
                nowEpochMillis = Clock.System.now().toEpochMilliseconds()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(session.isRunning, session.startedAtEpochMillis) {
        nowEpochMillis = Clock.System.now().toEpochMilliseconds()
        while (session.isRunning) {
            delay(STOPWATCH_TICK_MILLIS)
            nowEpochMillis = Clock.System.now().toEpochMilliseconds()
        }
    }
    val effectiveNowEpochMillis = if (session.isRunning) {
        maxOf(nowEpochMillis, Clock.System.now().toEpochMilliseconds())
    } else {
        nowEpochMillis
    }
    var showGoalDialog by remember { mutableStateOf(initialAdultGoalDialogForHome != null) }
    val isMatchCompleted = fixture.status == FixtureStatus.Completed
    val teamNames = fixture.displayTeamNames()

    if (showGoalDialog && !isMatchCompleted) {
        GoalCaptureDialog(
            requiresPlayerSelection = session.preset.requiresPlayerSelection,
            initialIsHomeScoringTeam = initialAdultGoalDialogForHome ?: true,
            homeTeamName = teamNames.home,
            awayTeamName = teamNames.away,
            matchSheet = content.matchSheet,
            half = session.activeHalf,
            minute = session.currentMatchMinute(effectiveNowEpochMillis),
            initialOwnGoal = initialAdultGoalOwnGoal,
            onDismiss = { showGoalDialog = false },
            onRecordGoal = { isHomeScoringTeam, member, ownGoal ->
                val timestamp = Clock.System.now().toEpochMilliseconds()
                nowEpochMillis = timestamp
                onRecordGoal(isHomeScoringTeam, member, ownGoal, timestamp)
                showGoalDialog = false
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PreMatchChecklistCard(
                profileId = profileId,
                homeTeamName = teamNames.home,
                awayTeamName = teamNames.away,
                matchSheet = content.matchSheet,
                isMatchCompleted = isMatchCompleted,
                checkingInRefereeId = content.checkingInRefereeId,
                onCheckInReferee = onCheckInReferee,
            )
        }
        item {
            RefereeTimerCard(
                session = session,
                nowEpochMillis = effectiveNowEpochMillis,
                isMatchCompleted = isMatchCompleted,
                onSelectPreset = onSelectPreset,
                onToggleTimer = {
                    val timestamp = Clock.System.now().toEpochMilliseconds()
                    nowEpochMillis = timestamp
                    onToggleTimer(timestamp)
                },
                onAdvancePhase = {
                    val timestamp = Clock.System.now().toEpochMilliseconds()
                    nowEpochMillis = timestamp
                    onAdvancePhase(timestamp)
                },
                onResetTimer = onResetTimer,
            )
        }
        item {
            RefereeGoalsCard(
                homeTeamName = teamNames.home,
                awayTeamName = teamNames.away,
                session = session,
                isMatchCompleted = isMatchCompleted,
                onOpenGoalDialog = {
                    nowEpochMillis = Clock.System.now().toEpochMilliseconds()
                    showGoalDialog = true
                },
                onRemoveGoal = onRemoveGoal,
                onToggleGoalOwnGoal = onToggleGoalOwnGoal,
            )
        }
        item {
            RefereeSubmitCard(
                session = session,
                isMatchCompleted = isMatchCompleted,
                isSubmitting = content.isSubmittingRefereeReport,
                submitted = content.refereeReportSubmitted,
                updateFailed = content.updateFailed,
                onSubmit = {
                    val timestamp = Clock.System.now().toEpochMilliseconds()
                    nowEpochMillis = timestamp
                    onSubmitReport(timestamp)
                },
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun PreMatchChecklistCard(
    profileId: String,
    homeTeamName: String,
    awayTeamName: String,
    matchSheet: MatchSheet,
    isMatchCompleted: Boolean,
    checkingInRefereeId: String?,
    onCheckInReferee: (String) -> Unit,
) {
    val bothTeamsEntered = matchSheet.homeTeam.status != TeamSheetStatus.Open &&
        matchSheet.awayTeam.status != TeamSheetStatus.Open
    val bothTeamsConfirmed = matchSheet.homeTeam.status == TeamSheetStatus.Confirmed &&
        matchSheet.awayTeam.status == TeamSheetStatus.Confirmed
    val refereeOfficial = matchSheet.referees.firstOrNull { it.userId == profileId }
        ?: matchSheet.referees.firstOrNull()
    val refereeCheckedIn = refereeOfficial?.checkedIn == true

    SportsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.referee_checklist_section_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            ChecklistItemRow(
                title = stringResource(Res.string.referee_checklist_teams_entered_title),
                subtitle = stringResource(
                    Res.string.referee_checklist_teams_entered_body,
                    homeTeamName,
                    teamStatusText(matchSheet.homeTeam.status),
                    awayTeamName,
                    teamStatusText(matchSheet.awayTeam.status),
                ),
                completed = bothTeamsEntered,
            )
            HorizontalDivider()
            ChecklistItemRow(
                title = stringResource(Res.string.referee_checklist_opposition_confirmed_title),
                subtitle = stringResource(
                    if (bothTeamsConfirmed) {
                        Res.string.referee_checklist_opposition_confirmed_ready
                    } else {
                        Res.string.referee_checklist_opposition_confirmed_pending
                    },
                ),
                completed = bothTeamsConfirmed,
            )
            HorizontalDivider()
            ChecklistItemRow(
                title = stringResource(Res.string.referee_checklist_checkin_title),
                subtitle = stringResource(
                    if (refereeCheckedIn) {
                        Res.string.referee_checklist_checkin_ready
                    } else {
                        Res.string.referee_checklist_checkin_pending
                    },
                ),
                completed = refereeCheckedIn,
                action = if (!refereeCheckedIn && refereeOfficial != null) {
                    {
                        val isCheckingIn = checkingInRefereeId == refereeOfficial.id
                        Button(
                            onClick = { onCheckInReferee(refereeOfficial.id) },
                            enabled = !isMatchCompleted && checkingInRefereeId == null,
                        ) {
                            if (isCheckingIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = stringResource(Res.string.referee_checklist_checkin_action),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun ChecklistItemRow(
    title: String,
    subtitle: String,
    completed: Boolean,
    action: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            ChecklistStatusPill(completed = completed)
        }
        if (action != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                action()
            }
        }
    }
}

@Composable
private fun ChecklistStatusPill(
    completed: Boolean,
) {
    Surface(
        shape = CircleShape,
        color = if (completed) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (completed) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
    ) {
        Text(
            text = stringResource(
                if (completed) {
                    Res.string.referee_checklist_status_done
                } else {
                    Res.string.referee_checklist_status_pending
                },
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
private fun RefereeTimerCard(
    session: RefereeMatchSession,
    nowEpochMillis: Long,
    isMatchCompleted: Boolean,
    onSelectPreset: (RefereeMatchPreset) -> Unit,
    onToggleTimer: () -> Unit,
    onAdvancePhase: () -> Unit,
    onResetTimer: () -> Unit,
) {
    val elapsedMillis = session.elapsedHalfMillis(nowEpochMillis)
    val targetHalfMillis = session.preset.halfDurationMinutes * MILLIS_PER_MINUTE
    val stoppageMillis = (elapsedMillis - targetHalfMillis).coerceAtLeast(0L)
    val canChangePreset = !isMatchCompleted && session.phase == RefereeTimerPhase.NotStarted

    SportsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.referee_timer_section_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = CircleShape,
                    color = if (session.isRunning) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (session.isRunning) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Text(
                        text = timerPhaseLabel(session.phase),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = stringResource(Res.string.referee_timer_preset_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RefereeMatchPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = session.preset == preset,
                        onClick = { onSelectPreset(preset) },
                        enabled = canChangePreset,
                        label = {
                            Text(
                                text = presetLabel(preset),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.88f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = formatElapsedMillis(elapsedMillis),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = stringResource(
                            Res.string.referee_timer_target,
                            session.preset.halfDurationMinutes,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (stoppageMillis > 0L) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Text(
                                text = stringResource(
                                    Res.string.referee_timer_stoppage,
                                    formatElapsedMillis(stoppageMillis),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (session.phase != RefereeTimerPhase.FullTime) {
                    Button(
                        onClick = onToggleTimer,
                        enabled = !isMatchCompleted,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = when {
                                session.isRunning -> stringResource(Res.string.referee_timer_pause)

                                session.phase == RefereeTimerPhase.NotStarted ->
                                    stringResource(Res.string.referee_timer_start)

                                session.phase == RefereeTimerPhase.HalfTime ->
                                    stringResource(Res.string.referee_timer_start_second_half)

                                else -> stringResource(Res.string.referee_timer_resume)
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                when (session.phase) {
                    RefereeTimerPhase.FirstHalf -> {
                        OutlinedButton(
                            onClick = onAdvancePhase,
                            enabled = !isMatchCompleted,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(Res.string.referee_timer_end_first_half),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    RefereeTimerPhase.SecondHalf -> {
                        OutlinedButton(
                            onClick = onAdvancePhase,
                            enabled = !isMatchCompleted,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(Res.string.referee_timer_end_match),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    RefereeTimerPhase.NotStarted,
                    RefereeTimerPhase.HalfTime,
                    RefereeTimerPhase.FullTime,
                    -> Unit
                }
            }

            if (session.phase != RefereeTimerPhase.NotStarted) {
                var confirmingReset by remember { mutableStateOf(false) }
                if (confirmingReset && !isMatchCompleted) {
                    AlertDialog(
                        onDismissRequest = { confirmingReset = false },
                        title = {
                            Text(
                                text = stringResource(Res.string.referee_timer_reset_confirm_title),
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        text = {
                            Text(stringResource(Res.string.referee_timer_reset_confirm_body))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    confirmingReset = false
                                    onResetTimer()
                                },
                            ) {
                                Text(
                                    text = stringResource(Res.string.action_confirm),
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmingReset = false }) {
                                Text(stringResource(Res.string.action_cancel))
                            }
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { confirmingReset = true },
                        enabled = !isMatchCompleted,
                    ) {
                        Text(stringResource(Res.string.referee_timer_reset))
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun RefereeGoalsCard(
    homeTeamName: String,
    awayTeamName: String,
    session: RefereeMatchSession,
    isMatchCompleted: Boolean,
    onOpenGoalDialog: () -> Unit,
    onRemoveGoal: (String) -> Unit,
    onToggleGoalOwnGoal: (String) -> Unit,
) {
    SportsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.referee_goals_section_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = stringResource(
                            Res.string.referee_goals_half_time_score,
                            session.homeHalfTimeScore,
                            session.awayHalfTimeScore,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.88f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = homeTeamName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(
                            Res.string.match_score,
                            session.homeFullTimeScore,
                            session.awayFullTimeScore,
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Text(
                        text = awayTeamName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Button(
                onClick = onOpenGoalDialog,
                enabled = !isMatchCompleted,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.SportsSoccer,
                    contentDescription = null,
                    size = 18.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.referee_goals_add_goal),
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = stringResource(
                    if (session.preset.requiresPlayerSelection) {
                        Res.string.referee_goals_adult_hint
                    } else {
                        Res.string.referee_goals_kids_hint
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (session.recordedGoals.isEmpty()) {
                Text(
                    text = stringResource(Res.string.referee_goals_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    session.recordedGoals.forEach { goal ->
                        RecordedGoalRow(
                            goal = goal,
                            homeTeamName = homeTeamName,
                            awayTeamName = awayTeamName,
                            isMatchCompleted = isMatchCompleted,
                            canToggleOwnGoal = !isMatchCompleted &&
                                (!session.preset.requiresPlayerSelection || goal.matchSheetMemberId == null),
                            onToggleOwnGoal = { onToggleGoalOwnGoal(goal.id) },
                            onRemove = { onRemoveGoal(goal.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun RecordedGoalRow(
    goal: RecordedRefereeGoal,
    homeTeamName: String,
    awayTeamName: String,
    isMatchCompleted: Boolean,
    canToggleOwnGoal: Boolean,
    onToggleOwnGoal: () -> Unit,
    onRemove: () -> Unit,
) {
    val scoringTeamName = if (goal.isHomeScoringTeam) homeTeamName else awayTeamName
    val playerSummary = when {
        !goal.jerseyNumber.isNullOrBlank() ->
            stringResource(
                Res.string.referee_goals_player_numbered,
                goal.jerseyNumber,
            )

        else -> stringResource(Res.string.referee_goals_team_goal, scoringTeamName)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    text = stringResource(
                        when (goal.half) {
                            MatchHalf.FirstHalf -> Res.string.referee_goals_half_first_short
                            MatchHalf.SecondHalf -> Res.string.referee_goals_half_second_short
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Text(
                text = stringResource(Res.string.referee_goals_minute, goal.minute),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playerSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = scoringTeamName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (goal.ownGoal || canToggleOwnGoal) {
                Surface(
                    shape = CircleShape,
                    color = if (goal.ownGoal) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (goal.ownGoal) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    border = if (goal.ownGoal) {
                        null
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
                    modifier = if (canToggleOwnGoal) {
                        Modifier.clickable(onClick = onToggleOwnGoal)
                    } else {
                        Modifier
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.referee_goals_own_goal_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
        IconButton(
            onClick = onRemove,
            enabled = !isMatchCompleted,
        ) {
            MaterialSymbolIcon(
                symbol = MaterialSymbol.Delete,
                contentDescription = stringResource(Res.string.referee_goals_remove),
                size = 20.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun RefereeSubmitCard(
    session: RefereeMatchSession,
    isMatchCompleted: Boolean,
    isSubmitting: Boolean,
    submitted: Boolean,
    updateFailed: Boolean,
    onSubmit: () -> Unit,
) {
    SportsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.referee_submit_section_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (session.preset.requiresPlayerSelection) {
                    stringResource(
                        Res.string.referee_submit_adult_body,
                        session.homeFullTimeScore,
                        session.awayFullTimeScore,
                        session.recordedGoals.count { !it.matchSheetMemberId.isNullOrBlank() },
                    )
                } else {
                    stringResource(
                        Res.string.referee_submit_score_body,
                        session.homeHalfTimeScore,
                        session.awayHalfTimeScore,
                        session.homeFullTimeScore,
                        session.awayFullTimeScore,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (updateFailed) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.referee_submit_failed),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            if (submitted) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MaterialSymbolIcon(
                            symbol = MaterialSymbol.Check,
                            contentDescription = null,
                            size = 18.dp,
                        )
                        Text(
                            text = stringResource(Res.string.referee_submit_success),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Button(
                onClick = onSubmit,
                enabled = !isMatchCompleted && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(
                        if (session.preset.requiresPlayerSelection) {
                            Res.string.referee_submit_score_and_goals_button
                        } else {
                            Res.string.referee_submit_score_button
                        },
                    ),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
private fun GoalCaptureDialog(
    requiresPlayerSelection: Boolean,
    initialIsHomeScoringTeam: Boolean,
    homeTeamName: String,
    awayTeamName: String,
    matchSheet: MatchSheet,
    half: MatchHalf,
    minute: Int,
    onDismiss: () -> Unit,
    onRecordGoal: (Boolean, MatchSheetMember?, Boolean) -> Unit,
    initialOwnGoal: Boolean = false,
) {
    var isHomeScoringTeam by remember { mutableStateOf(initialIsHomeScoringTeam) }
    var ownGoal by remember { mutableStateOf(initialOwnGoal) }
    val scoringTeamName = if (isHomeScoringTeam) homeTeamName else awayTeamName
    val oppositionTeamName = if (isHomeScoringTeam) awayTeamName else homeTeamName
    val activeJerseyTeamName = if (ownGoal) oppositionTeamName else scoringTeamName
    val activeJerseyTeam = if (ownGoal == isHomeScoringTeam) {
        matchSheet.awayTeam
    } else {
        matchSheet.homeTeam
    }
    val playerSourceTeamId = activeJerseyTeam.id
    val teamPrimaryColor = activeJerseyTeam.primaryColor.toColorOrNull()
    val teamSecondaryColor = activeJerseyTeam.secondaryColor.toColorOrNull()
    val teamMembers = remember(matchSheet.members, playerSourceTeamId) {
        val numberedForTeam = matchSheet.members
            .filter { it.teamId == playerSourceTeamId && !it.jerseyNumber.isNullOrBlank() }
        val availableForTeam = numberedForTeam.filter { it.available }
        (availableForTeam.ifEmpty { numberedForTeam }).sortedWith(
            compareBy(
                { it.jerseyNumber?.toIntOrNull() ?: Int.MAX_VALUE },
                { it.displayName },
            ),
        )
    }
    val homeJerseyTeamId = if (ownGoal) matchSheet.awayTeam.id else matchSheet.homeTeam.id
    val awayJerseyTeamId = if (ownGoal) matchSheet.homeTeam.id else matchSheet.awayTeam.id
    val homeHasShirtNumbers = remember(matchSheet.members, homeJerseyTeamId) {
        matchSheet.members.any { it.teamId == homeJerseyTeamId && !it.jerseyNumber.isNullOrBlank() }
    }
    val awayHasShirtNumbers = remember(matchSheet.members, awayJerseyTeamId) {
        matchSheet.members.any { it.teamId == awayJerseyTeamId && !it.jerseyNumber.isNullOrBlank() }
    }
    val showShirtNumberButtons = requiresPlayerSelection && teamMembers.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = DIALOG_SCRIM_ALPHA))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {},
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (showShirtNumberButtons) {
                                stringResource(
                                    Res.string.referee_goal_dialog_title,
                                    scoringTeamName,
                                )
                            } else {
                                stringResource(Res.string.referee_goal_dialog_kids_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(
                                Res.string.referee_goal_dialog_subtitle,
                                halfLabel(half),
                                minute,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text(
                        text = stringResource(Res.string.referee_goal_dialog_select_team),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GoalDialogTeamButton(
                            teamName = homeTeamName,
                            primaryColorHex = matchSheet.homeTeam.primaryColor,
                            secondaryColorHex = matchSheet.homeTeam.secondaryColor,
                            selected = if (showShirtNumberButtons) isHomeScoringTeam else null,
                            onClick = {
                                if (requiresPlayerSelection && homeHasShirtNumbers) {
                                    isHomeScoringTeam = true
                                } else {
                                    onRecordGoal(true, null, ownGoal)
                                }
                            },
                        )
                        GoalDialogTeamButton(
                            teamName = awayTeamName,
                            primaryColorHex = matchSheet.awayTeam.primaryColor,
                            secondaryColorHex = matchSheet.awayTeam.secondaryColor,
                            selected = if (showShirtNumberButtons) !isHomeScoringTeam else null,
                            onClick = {
                                if (requiresPlayerSelection && awayHasShirtNumbers) {
                                    isHomeScoringTeam = false
                                } else {
                                    onRecordGoal(false, null, ownGoal)
                                }
                            },
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { ownGoal = !ownGoal }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(Res.string.referee_goal_dialog_own_goal),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Switch(
                                checked = ownGoal,
                                onCheckedChange = { ownGoal = it },
                            )
                        }
                    }

                    if (showShirtNumberButtons) {
                        Text(
                            text = stringResource(
                                Res.string.referee_goal_dialog_select_player,
                                activeJerseyTeamName,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (ownGoal) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            fontWeight = FontWeight.Bold,
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(JERSEY_GRID_COLUMNS),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(teamMembers, key = { it.id }) { member ->
                                val containerColor = teamPrimaryColor ?: if (ownGoal) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                                val contentColor = teamPrimaryColor?.contrastingContentColor()
                                    ?: if (ownGoal) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                val borderColor = if (teamPrimaryColor != null) {
                                    teamSecondaryColor?.takeIf { it != teamPrimaryColor }
                                        ?: contentColor.copy(alpha = TEAM_BORDER_CONTRAST_ALPHA)
                                } else if (ownGoal) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = containerColor,
                                    contentColor = contentColor,
                                    border = BorderStroke(1.5.dp, borderColor),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .clickable {
                                            onRecordGoal(isHomeScoringTeam, member, ownGoal)
                                        },
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        Text(
                                            text = "#${member.jerseyNumber.orEmpty()}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = stringResource(Res.string.action_cancel),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalDialogTeamButton(
    teamName: String,
    primaryColorHex: String?,
    secondaryColorHex: String?,
    selected: Boolean?,
    onClick: () -> Unit,
) {
    val teamPrimary = primaryColorHex.toColorOrNull()
    val teamSecondary = secondaryColorHex.toColorOrNull()
    val isActive = selected ?: true
    val containerColor = when {
        !isActive -> MaterialTheme.colorScheme.surfaceContainerHigh
        teamPrimary != null -> teamPrimary
        else -> MaterialTheme.colorScheme.primary
    }
    val contentColor = when {
        !isActive -> MaterialTheme.colorScheme.onSurface
        teamPrimary != null -> teamPrimary.contrastingContentColor()
        else -> MaterialTheme.colorScheme.onPrimary
    }
    val borderColor = when {
        !isActive -> teamPrimary ?: MaterialTheme.colorScheme.outlineVariant

        teamPrimary != null ->
            teamSecondary?.takeIf { it != teamPrimary }
                ?: contentColor.copy(alpha = TEAM_BORDER_CONTRAST_ALPHA)

        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(if (selected == true) 2.dp else 1.5.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MaterialSymbolIcon(
                symbol = if (selected == true) {
                    MaterialSymbol.Check
                } else {
                    MaterialSymbol.SportsSoccer
                },
                contentDescription = null,
                size = 18.dp,
            )
            Text(
                text = teamName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun teamStatusText(
    status: TeamSheetStatus,
): String = stringResource(
    when (status) {
        TeamSheetStatus.Open -> Res.string.match_sheet_open
        TeamSheetStatus.Submitted -> Res.string.match_sheet_submitted
        TeamSheetStatus.Confirmed -> Res.string.match_sheet_confirmed
    },
)

@Composable
private fun presetLabel(
    preset: RefereeMatchPreset,
): String = stringResource(
    when (preset) {
        RefereeMatchPreset.Under8s -> Res.string.referee_timer_preset_u8
        RefereeMatchPreset.Under18s -> Res.string.referee_timer_preset_u18
        RefereeMatchPreset.Adult -> Res.string.referee_timer_preset_adult
    },
)

@Composable
private fun timerPhaseLabel(
    phase: RefereeTimerPhase,
): String = stringResource(
    when (phase) {
        RefereeTimerPhase.NotStarted -> Res.string.referee_timer_phase_not_started
        RefereeTimerPhase.FirstHalf -> Res.string.referee_timer_phase_first_half
        RefereeTimerPhase.HalfTime -> Res.string.referee_timer_phase_half_time
        RefereeTimerPhase.SecondHalf -> Res.string.referee_timer_phase_second_half
        RefereeTimerPhase.FullTime -> Res.string.referee_timer_phase_full_time
    },
)

@Composable
private fun halfLabel(
    half: MatchHalf,
): String = stringResource(
    when (half) {
        MatchHalf.FirstHalf -> Res.string.referee_timer_phase_first_half
        MatchHalf.SecondHalf -> Res.string.referee_timer_phase_second_half
    },
)

internal fun formatElapsedMillis(
    elapsedMillis: Long,
): String {
    val totalSeconds = (elapsedMillis.coerceAtLeast(0L) / MILLIS_PER_SECOND).toInt()
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

internal fun Color.contrastingContentColor(): Color =
    if (luminance() > TEAM_COLOR_LUMINANCE_THRESHOLD) Color.Black else Color.White

internal const val REFEREE_CHECKLIST_LEAD_MILLIS = 30L * 60L * 1_000L
internal const val REFEREE_CHECKLIST_TRAIL_MILLIS = 18L * 60L * 60L * 1_000L
private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60
private const val STOPWATCH_TICK_MILLIS = 250L
private const val JERSEY_GRID_COLUMNS = 4
private const val DIALOG_SCRIM_ALPHA = 0.72f
private const val TEAM_COLOR_LUMINANCE_THRESHOLD = 0.35f
private const val TEAM_BORDER_CONTRAST_ALPHA = 0.36f
