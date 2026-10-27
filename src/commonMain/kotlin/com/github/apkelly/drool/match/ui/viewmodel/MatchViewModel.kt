package com.github.apkelly.drool.match.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import com.github.apkelly.drool.common.domain.model.MatchSheet
import com.github.apkelly.drool.common.domain.model.MatchSheetGoalDraft
import com.github.apkelly.drool.common.domain.model.MatchSheetMember
import com.github.apkelly.drool.common.domain.model.OperationResult
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
import com.github.apkelly.drool.common.logging.DroolLog
import com.github.apkelly.drool.common.ui.widgets.mergeRefereeAllocationFrom
import com.github.apkelly.drool.match.domain.model.RecordedRefereeGoal
import com.github.apkelly.drool.match.domain.model.RefereeMatchPreset
import com.github.apkelly.drool.match.domain.model.RefereeMatchSession
import com.github.apkelly.drool.match.domain.model.RefereeTimerPhase
import com.github.apkelly.drool.match.domain.usecase.CheckInRefereeUseCase
import com.github.apkelly.drool.match.domain.usecase.LoadMatchDetailsUseCase
import com.github.apkelly.drool.match.domain.usecase.LoadMatchSheetUseCase
import com.github.apkelly.drool.match.domain.usecase.LoadMatchWeatherUseCase
import com.github.apkelly.drool.match.domain.usecase.RefereeMatchSessionUseCase
import com.github.apkelly.drool.match.domain.usecase.SubmitRefereeMatchReportUseCase
import com.github.apkelly.drool.match.domain.usecase.UpdateMatchSheetMemberUseCase
import com.github.apkelly.drool.match.domain.usecase.UpdateTeamSheetStatusUseCase
import com.github.apkelly.drool.match.ui.model.MatchDetailsUiState
import com.github.apkelly.drool.match.ui.model.MatchSheetUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "TooManyFunctions")
class MatchViewModel(
    private val loadMatchDetails: LoadMatchDetailsUseCase,
    private val loadMatchWeather: LoadMatchWeatherUseCase,
    private val loadMatchSheetUseCase: LoadMatchSheetUseCase,
    private val updateMatchSheetMemberUseCase: UpdateMatchSheetMemberUseCase,
    private val updateTeamSheetStatusUseCase: UpdateTeamSheetStatusUseCase,
    private val matchCache: MatchCache,
    private val checkInRefereeUseCase: CheckInRefereeUseCase? = null,
    private val submitRefereeMatchReportUseCase: SubmitRefereeMatchReportUseCase? = null,
    private val refereeMatchSessionUseCase: RefereeMatchSessionUseCase? = null,
) : ViewModel() {
    private val logger = DroolLog.withTag("MatchViewModel")
    private val mutableState = MutableStateFlow<MatchDetailsUiState>(MatchDetailsUiState.Idle)
    private val mutableMatchSheetState =
        MutableStateFlow<MatchSheetUiState>(MatchSheetUiState.Idle)
    private var ownerAccountId: String? = null
    val state: StateFlow<MatchDetailsUiState> = mutableState
    val matchSheetState: StateFlow<MatchSheetUiState> = mutableMatchSheetState

    fun configureAccount(
        accountId: String,
    ) {
        val previousAccountId = ownerAccountId
        ownerAccountId = accountId
        matchCache.configureAccount(accountId)
        if (previousAccountId != null && previousAccountId != accountId) {
            mutableState.value = MatchDetailsUiState.Idle
            mutableMatchSheetState.value = MatchSheetUiState.Idle
        }
    }

    fun load(
        profileId: String,
        matchId: String,
    ) = request(profileId, matchId, force = false)

    fun refresh(
        profileId: String,
        matchId: String,
    ) = request(profileId, matchId, force = true)

    fun seedFixture(
        fixture: Fixture,
    ) {
        val profileId = fixture.profileId ?: return
        val key = MatchCacheKey(profileId, fixture.id)
        val existing = matchCache.get(key)
        val merged = existing?.mergeRefereeAllocationFrom(fixture) ?: fixture
        matchCache.put(key, merged, resolvedDetails = matchCache.isResolved(key))
    }

    fun loadMatchSheet(
        profileId: String,
        force: Boolean = false,
    ) {
        val details = mutableState.value as? MatchDetailsUiState.Content ?: return
        val matchId = details.matchId
        val key = MatchCacheKey(profileId, matchId)
        val current = (mutableMatchSheetState.value as? MatchSheetUiState.Content)
            ?.takeIf { it.profileId == profileId && it.matchId == matchId }
        val isLoading =
            mutableMatchSheetState.value == MatchSheetUiState.Loading(profileId, matchId)
        if (!force && (current != null || isLoading)) return
        val loadingState = MatchSheetUiState.Loading(profileId, matchId)
        if (current == null) {
            mutableMatchSheetState.value = loadingState
        }
        viewModelScope.launch {
            val savedSession = current?.refereeSession
                ?: matchCache.getRefereeSession(key)
                ?: refereeMatchSessionUseCase?.load(profileId, matchId)
            when (
                val result = loadMatchSheetUseCase(profileId, details.fixture)
            ) {
                is OperationResult.Success -> {
                    val latestContent =
                        (mutableMatchSheetState.value as? MatchSheetUiState.Content)
                            ?.takeIf { it.profileId == profileId && it.matchId == matchId }
                    val resolvedSession = latestContent?.refereeSession
                        ?: matchCache.getRefereeSession(key)
                        ?: savedSession
                        ?: refereeMatchSessionUseCase?.load(profileId, matchId)
                        ?: seedRefereeSessionFromMatchSheet(
                            profileId = profileId,
                            matchId = matchId,
                            matchSheet = result.value,
                        )
                    matchCache.putRefereeSession(key, resolvedSession)
                    if (
                        mutableMatchSheetState.value == loadingState ||
                        latestContent != null
                    ) {
                        mutableMatchSheetState.value = MatchSheetUiState.Content(
                            profileId = profileId,
                            matchId = matchId,
                            matchSheet = result.value,
                            refereeSession = resolvedSession,
                            refereeReportSubmitted =
                            resolvedSession.submittedAtEpochMillis != null,
                        )
                    }
                }

                is OperationResult.Failed -> {
                    logger.w { "Unable to load match sheet: ${result.reason}" }
                    val latestContent =
                        (mutableMatchSheetState.value as? MatchSheetUiState.Content)
                            ?.takeIf { it.profileId == profileId && it.matchId == matchId }
                    if (latestContent != null) {
                        mutableMatchSheetState.value = latestContent.copy(updateFailed = true)
                    } else if (mutableMatchSheetState.value == loadingState) {
                        mutableMatchSheetState.value = MatchSheetUiState.Failed(profileId, matchId)
                    }
                }
            }
        }
    }

    fun updateMemberAvailability(
        profileId: String,
        memberId: String,
        available: Boolean,
    ) {
        val current = mutableMatchSheetState.value as? MatchSheetUiState.Content ?: return
        if (current.updatingMemberId != null || current.updatingTeamId != null) return
        mutableMatchSheetState.value = current.copy(
            updatingMemberId = memberId,
            updateFailed = false,
        )
        viewModelScope.launch {
            when (val result = updateMatchSheetMemberUseCase(profileId, memberId, available)) {
                is OperationResult.Success -> {
                    val latest = mutableMatchSheetState.value as? MatchSheetUiState.Content
                        ?: return@launch
                    mutableMatchSheetState.value = latest.copy(
                        matchSheet = latest.matchSheet.copy(
                            members = latest.matchSheet.members.map { member ->
                                if (member.id == memberId) result.value else member
                            },
                        ),
                        updatingMemberId = null,
                    )
                }

                is OperationResult.Failed -> {
                    logger.w { "Unable to update match sheet member: ${result.reason}" }
                    val latest = mutableMatchSheetState.value as? MatchSheetUiState.Content
                        ?: return@launch
                    mutableMatchSheetState.value = latest.copy(
                        updatingMemberId = null,
                        updateFailed = true,
                    )
                }
            }
        }
    }

    fun updateTeamStatus(
        profileId: String,
        teamId: String,
        action: TeamSheetAction,
    ) {
        val current = mutableMatchSheetState.value as? MatchSheetUiState.Content ?: return
        if (current.updatingMemberId != null || current.updatingTeamId != null) return
        mutableMatchSheetState.value = current.copy(
            updatingTeamId = teamId,
            updateFailed = false,
        )
        viewModelScope.launch {
            when (
                val result = updateTeamSheetStatusUseCase(
                    profileId = profileId,
                    matchSheetId = current.matchSheet.id,
                    teamId = teamId,
                    action = action,
                )
            ) {
                is OperationResult.Success -> {
                    val latest = mutableMatchSheetState.value as? MatchSheetUiState.Content
                        ?: return@launch
                    val sheet = latest.matchSheet
                    mutableMatchSheetState.value = latest.copy(
                        matchSheet = sheet.copy(
                            homeTeam = sheet.homeTeam.takeUnless { it.id == teamId }
                                ?: sheet.homeTeam.copy(status = result.value),
                            awayTeam = sheet.awayTeam.takeUnless { it.id == teamId }
                                ?: sheet.awayTeam.copy(status = result.value),
                        ),
                        updatingTeamId = null,
                    )
                }

                is OperationResult.Failed -> {
                    logger.w { "Unable to update team sheet status: ${result.reason}" }
                    val latest = mutableMatchSheetState.value as? MatchSheetUiState.Content
                        ?: return@launch
                    mutableMatchSheetState.value = latest.copy(
                        updatingTeamId = null,
                        updateFailed = true,
                    )
                }
            }
        }
    }

    fun checkInReferee(
        profileId: String,
        refereeOfficialId: String,
    ) {
        val checkIn = checkInRefereeUseCase ?: return
        val details = mutableState.value as? MatchDetailsUiState.Content
        if (details?.fixture?.status == FixtureStatus.Completed) return
        val current = mutableMatchSheetState.value as? MatchSheetUiState.Content ?: return
        if (current.checkingInRefereeId != null) return
        mutableMatchSheetState.value = current.copy(
            checkingInRefereeId = refereeOfficialId,
            updateFailed = false,
        )
        viewModelScope.launch {
            when (val result = checkIn(profileId, refereeOfficialId, checkedIn = true)) {
                is OperationResult.Success -> {
                    val latest = mutableMatchSheetState.value as? MatchSheetUiState.Content
                        ?: return@launch
                    mutableMatchSheetState.value = latest.copy(
                        matchSheet = latest.matchSheet.copy(
                            referees = latest.matchSheet.referees.map { official ->
                                if (official.id == refereeOfficialId) {
                                    official.copy(checkedIn = true)
                                } else {
                                    official
                                }
                            },
                        ),
                        checkingInRefereeId = null,
                    )
                }

                is OperationResult.Failed -> {
                    logger.w { "Unable to check in referee: ${result.reason}" }
                    val latest = mutableMatchSheetState.value as? MatchSheetUiState.Content
                        ?: return@launch
                    mutableMatchSheetState.value = latest.copy(
                        checkingInRefereeId = null,
                        updateFailed = true,
                    )
                }
            }
        }
    }

    fun selectRefereePreset(
        profileId: String,
        preset: RefereeMatchPreset,
    ) {
        updateRefereeSession(profileId) { session ->
            if (session.phase != RefereeTimerPhase.NotStarted) {
                session
            } else {
                session.copy(
                    preset = preset,
                    submittedAtEpochMillis = null,
                )
            }
        }
    }

    fun toggleRefereeTimer(
        profileId: String,
        nowEpochMillis: Long,
    ) {
        updateRefereeSession(profileId) { session ->
            if (session.isRunning) {
                session.copy(
                    accumulatedMillis = session.elapsedHalfMillis(nowEpochMillis),
                    isRunning = false,
                    startedAtEpochMillis = null,
                )
            } else {
                when (session.phase) {
                    RefereeTimerPhase.NotStarted -> session.copy(
                        phase = RefereeTimerPhase.FirstHalf,
                        accumulatedMillis = 0L,
                        isRunning = true,
                        startedAtEpochMillis = nowEpochMillis,
                    )

                    RefereeTimerPhase.HalfTime -> session.copy(
                        phase = RefereeTimerPhase.SecondHalf,
                        accumulatedMillis = 0L,
                        isRunning = true,
                        startedAtEpochMillis = nowEpochMillis,
                    )

                    RefereeTimerPhase.FullTime -> session.copy(
                        phase = RefereeTimerPhase.SecondHalf,
                        isRunning = true,
                        startedAtEpochMillis = nowEpochMillis,
                    )

                    RefereeTimerPhase.FirstHalf,
                    RefereeTimerPhase.SecondHalf,
                    -> session.copy(
                        isRunning = true,
                        startedAtEpochMillis = nowEpochMillis,
                    )
                }
            }
        }
    }

    fun advanceRefereePhase(
        profileId: String,
        nowEpochMillis: Long,
    ) {
        updateRefereeSession(profileId) { session ->
            when (session.phase) {
                RefereeTimerPhase.NotStarted -> session.copy(
                    phase = RefereeTimerPhase.FirstHalf,
                    accumulatedMillis = 0L,
                    isRunning = true,
                    startedAtEpochMillis = nowEpochMillis,
                )

                RefereeTimerPhase.FirstHalf -> session.copy(
                    phase = RefereeTimerPhase.HalfTime,
                    accumulatedMillis = 0L,
                    isRunning = false,
                    startedAtEpochMillis = null,
                )

                RefereeTimerPhase.HalfTime -> session.copy(
                    phase = RefereeTimerPhase.SecondHalf,
                    accumulatedMillis = 0L,
                    isRunning = true,
                    startedAtEpochMillis = nowEpochMillis,
                )

                RefereeTimerPhase.SecondHalf -> session.copy(
                    phase = RefereeTimerPhase.FullTime,
                    accumulatedMillis = session.elapsedHalfMillis(nowEpochMillis),
                    isRunning = false,
                    startedAtEpochMillis = null,
                )

                RefereeTimerPhase.FullTime -> session
            }
        }
    }

    fun resetRefereeTimer(
        profileId: String,
    ) {
        updateRefereeSession(profileId) { session ->
            session.copy(
                phase = RefereeTimerPhase.NotStarted,
                isRunning = false,
                accumulatedMillis = 0L,
                startedAtEpochMillis = null,
            )
        }
    }

    fun recordRefereeGoal(
        profileId: String,
        isHomeScoringTeam: Boolean,
        member: MatchSheetMember?,
        ownGoal: Boolean,
        nowEpochMillis: Long,
    ) {
        updateRefereeSession(profileId, clearSubmitted = true) { session ->
            val goal = RecordedRefereeGoal(
                id = "goal-$nowEpochMillis-${session.recordedGoals.size}",
                isHomeScoringTeam = isHomeScoringTeam,
                half = session.activeHalf,
                minute = session.currentMatchMinute(nowEpochMillis),
                matchSheetMemberId = member?.id,
                playerName = member?.displayName,
                jerseyNumber = member?.jerseyNumber,
                ownGoal = ownGoal,
            )
            session.copy(
                recordedGoals = session.recordedGoals + goal,
                submittedAtEpochMillis = null,
            )
        }
    }

    fun removeRefereeGoal(
        profileId: String,
        goalId: String,
    ) {
        updateRefereeSession(profileId, clearSubmitted = true) { session ->
            session.copy(
                recordedGoals = session.recordedGoals.filterNot { it.id == goalId },
                submittedAtEpochMillis = null,
            )
        }
    }

    fun toggleRefereeGoalOwnGoal(
        profileId: String,
        goalId: String,
    ) {
        updateRefereeSession(profileId, clearSubmitted = true) { session ->
            session.copy(
                recordedGoals = session.recordedGoals.map { goal ->
                    if (goal.id == goalId) {
                        goal.copy(ownGoal = !goal.ownGoal)
                    } else {
                        goal
                    }
                },
                submittedAtEpochMillis = null,
            )
        }
    }

    fun submitRefereeMatchReport(
        profileId: String,
        nowEpochMillis: Long,
    ) {
        val submitReport = submitRefereeMatchReportUseCase ?: return
        val details = mutableState.value as? MatchDetailsUiState.Content ?: return
        if (details.fixture.status == FixtureStatus.Completed) return
        val current = mutableMatchSheetState.value as? MatchSheetUiState.Content ?: return
        if (current.profileId != profileId || current.isSubmittingRefereeReport) return
        val session = current.refereeSession
        val goalDrafts = if (session.preset.requiresPlayerSelection) {
            session.recordedGoals.mapNotNull { goal ->
                val memberId = goal.matchSheetMemberId?.takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
                MatchSheetGoalDraft(
                    matchSheetMemberId = memberId,
                    half = goal.half,
                    minute = goal.minute,
                    ownGoal = goal.ownGoal,
                )
            }
        } else {
            null
        }
        mutableMatchSheetState.value = current.copy(
            isSubmittingRefereeReport = true,
            refereeReportSubmitted = false,
            updateFailed = false,
        )
        viewModelScope.launch {
            val result = submitReport(
                profileId = profileId,
                fixture = details.fixture,
                scoreId = current.matchSheet.score?.id,
                homeHalfTime = session.homeHalfTimeScore,
                awayHalfTime = session.awayHalfTimeScore,
                homeFullTime = session.homeFullTimeScore,
                awayFullTime = session.awayFullTimeScore,
                goals = goalDrafts,
                existingGoalIds = current.matchSheet.goals.map { it.id },
            )
            when (result) {
                is OperationResult.Success -> {
                    val updatedSession = session.copy(submittedAtEpochMillis = nowEpochMillis)
                    matchCache.putRefereeSession(
                        MatchCacheKey(profileId, details.matchId),
                        updatedSession,
                    )
                    refereeMatchSessionUseCase?.save(updatedSession)
                    val latestSheet = mutableMatchSheetState.value as? MatchSheetUiState.Content
                    if (latestSheet?.profileId == profileId && latestSheet.matchId == details.matchId) {
                        mutableMatchSheetState.value = latestSheet.copy(
                            matchSheet = result.value,
                            isSubmittingRefereeReport = false,
                            refereeReportSubmitted = true,
                            refereeSession = updatedSession,
                            updateFailed = false,
                        )
                    }
                    val latestDetails = mutableState.value as? MatchDetailsUiState.Content
                    if (latestDetails?.profileId == profileId && latestDetails.matchId == details.matchId) {
                        val updatedFixture = latestDetails.fixture.copy(
                            homeScore = session.homeFullTimeScore,
                            awayScore = session.awayFullTimeScore,
                        )
                        matchCache.put(
                            MatchCacheKey(profileId, details.matchId),
                            updatedFixture,
                            resolvedDetails = true,
                        )
                        mutableState.value = latestDetails.copy(fixture = updatedFixture)
                    }
                }

                is OperationResult.Failed -> {
                    logger.w { "Unable to submit referee match report: ${result.reason}" }
                    val latestSheet = mutableMatchSheetState.value as? MatchSheetUiState.Content
                    if (latestSheet?.profileId == profileId && latestSheet.matchId == details.matchId) {
                        mutableMatchSheetState.value = latestSheet.copy(
                            isSubmittingRefereeReport = false,
                            updateFailed = true,
                        )
                    }
                }
            }
        }
    }

    private fun updateRefereeSession(
        profileId: String,
        clearSubmitted: Boolean = false,
        transform: (RefereeMatchSession) -> RefereeMatchSession,
    ) {
        val details = mutableState.value as? MatchDetailsUiState.Content
        if (details?.fixture?.status == FixtureStatus.Completed) return
        val current = mutableMatchSheetState.value as? MatchSheetUiState.Content ?: return
        if (current.profileId != profileId) return
        val updated = transform(current.refereeSession)
        matchCache.putRefereeSession(MatchCacheKey(profileId, current.matchId), updated)
        mutableMatchSheetState.value = current.copy(
            refereeSession = updated,
            refereeReportSubmitted = if (clearSubmitted) false else current.refereeReportSubmitted,
            updateFailed = false,
        )
        viewModelScope.launch {
            refereeMatchSessionUseCase?.save(updated)
        }
    }

    private fun request(
        profileId: String,
        matchId: String,
        force: Boolean,
    ) {
        val key = MatchCacheKey(profileId, matchId)
        val current = (mutableState.value as? MatchDetailsUiState.Content)
            ?.takeIf { it.profileId == profileId && it.matchId == matchId }
        if (!force && matchCache.isResolved(key) && current != null) return
        if (force && current?.isRefreshing == true) return
        if (mutableState.value == MatchDetailsUiState.Loading(profileId, matchId)) return

        if (!isMatchSheetFor(profileId, matchId)) {
            mutableMatchSheetState.value = MatchSheetUiState.Idle
        }

        val cached = current?.fixture ?: matchCache.get(key)
        when {
            force && current != null -> mutableState.value = current.copy(
                isRefreshing = true,
                refreshFailed = false,
            )

            cached != null ->
                mutableState.value =
                    MatchDetailsUiState.Content(profileId, matchId, cached)

            else -> mutableState.value = MatchDetailsUiState.Loading(profileId, matchId)
        }

        viewModelScope.launch {
            var refreshFailed = false
            val fixture = when (val result = loadMatchDetails(profileId, matchId)) {
                is OperationResult.Success ->
                    result.value
                        .mergeRefereeAllocationFrom(cached)
                        .also {
                            matchCache.put(key, it, resolvedDetails = true)
                        }

                is OperationResult.Failed -> {
                    logger.w { "Unable to load match details: ${result.reason}" }
                    if (cached == null) {
                        if (isDetailsTarget(profileId, matchId)) {
                            mutableState.value = MatchDetailsUiState.Failed(profileId, matchId)
                        }
                        return@launch
                    }
                    refreshFailed = true
                    cached
                }
            }
            if (!isDetailsTarget(profileId, matchId)) return@launch
            mutableState.value = MatchDetailsUiState.Content(
                profileId = profileId,
                matchId = matchId,
                fixture = fixture,
                weather = current?.weather,
                isWeatherLoading = fixture.latitude != null && fixture.longitude != null,
                isRefreshing = force,
                refreshFailed = refreshFailed,
            )
            val weather = when (val result = loadMatchWeather(fixture, force)) {
                is OperationResult.Success -> result.value

                is OperationResult.Failed -> {
                    logger.w { "Unable to load match weather: ${result.reason}" }
                    if (force) {
                        refreshFailed = true
                    }
                    current?.weather
                }
            }
            val latest = mutableState.value as? MatchDetailsUiState.Content
            if (latest?.profileId == profileId && latest.matchId == matchId) {
                mutableState.value = latest.copy(
                    weather = weather,
                    isWeatherLoading = false,
                    weatherUnavailable = weather == null,
                    isRefreshing = false,
                    refreshFailed = refreshFailed,
                )
            }
        }
    }

    private fun isDetailsTarget(
        profileId: String,
        matchId: String,
    ): Boolean = when (val current = mutableState.value) {
        is MatchDetailsUiState.Loading ->
            current.profileId == profileId && current.matchId == matchId

        is MatchDetailsUiState.Content ->
            current.profileId == profileId && current.matchId == matchId

        is MatchDetailsUiState.Failed,
        MatchDetailsUiState.Idle,
        -> false
    }

    private fun isMatchSheetFor(
        profileId: String,
        matchId: String,
    ): Boolean = when (val current = mutableMatchSheetState.value) {
        is MatchSheetUiState.Loading ->
            current.profileId == profileId && current.matchId == matchId

        is MatchSheetUiState.Content ->
            current.profileId == profileId && current.matchId == matchId

        is MatchSheetUiState.Failed ->
            current.profileId == profileId && current.matchId == matchId

        MatchSheetUiState.Idle -> true
    }
}

private fun seedRefereeSessionFromMatchSheet(
    profileId: String,
    matchId: String,
    matchSheet: MatchSheet,
): RefereeMatchSession {
    if (matchSheet.goals.isNotEmpty()) {
        val goals = matchSheet.goals.map { goal ->
            val isHomeTeamGoal = when {
                goal.ownGoal -> goal.teamId == matchSheet.awayTeam.id
                else -> goal.teamId == matchSheet.homeTeam.id
            }
            RecordedRefereeGoal(
                id = goal.id,
                isHomeScoringTeam = isHomeTeamGoal,
                half = goal.half,
                minute = goal.minute ?: 1,
                matchSheetMemberId = goal.matchSheetMemberId,
                playerName = goal.memberName,
                jerseyNumber = goal.jerseyNumber,
                ownGoal = goal.ownGoal,
            )
        }
        return RefereeMatchSession(
            profileId = profileId,
            matchId = matchId,
            preset = RefereeMatchPreset.Adult,
            recordedGoals = goals,
        )
    }
    return RefereeMatchSession(
        profileId = profileId,
        matchId = matchId,
    )
}
