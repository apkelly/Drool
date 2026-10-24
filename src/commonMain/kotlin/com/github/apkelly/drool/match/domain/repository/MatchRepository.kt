package com.github.apkelly.drool.match.domain.repository

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.MatchSheet
import com.github.apkelly.drool.common.domain.model.MatchSheetGoalDraft
import com.github.apkelly.drool.common.domain.model.MatchSheetMember
import com.github.apkelly.drool.common.domain.model.MatchWeather
import com.github.apkelly.drool.common.domain.model.OperationResult
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
import com.github.apkelly.drool.common.domain.model.TeamSheetStatus
import com.github.apkelly.drool.match.domain.model.RefereeMatchSession

interface MatchRepository {
    suspend fun loadMatchDetails(
        profileId: String,
        matchId: String,
    ): OperationResult<Fixture>
    suspend fun loadMatchWeather(
        fixture: Fixture,
        force: Boolean = false,
    ): OperationResult<MatchWeather?>
    suspend fun loadMatchSheet(
        profileId: String,
        fixture: Fixture,
    ): OperationResult<MatchSheet>
    suspend fun updateMemberAvailability(
        profileId: String,
        memberId: String,
        available: Boolean,
    ): OperationResult<MatchSheetMember>
    suspend fun updateTeamSheetStatus(
        profileId: String,
        matchSheetId: String,
        teamId: String,
        action: TeamSheetAction,
    ): OperationResult<TeamSheetStatus>
    suspend fun updateRefereeCheckIn(
        profileId: String,
        refereeOfficialId: String,
        checkedIn: Boolean,
    ): OperationResult<Unit>
    suspend fun submitRefereeMatchReport(
        profileId: String,
        fixture: Fixture,
        scoreId: String?,
        homeHalfTime: Int?,
        awayHalfTime: Int?,
        homeFullTime: Int,
        awayFullTime: Int,
        goals: List<MatchSheetGoalDraft>?,
        existingGoalIds: List<String>,
    ): OperationResult<MatchSheet>
    suspend fun loadRefereeMatchSession(
        profileId: String,
        matchId: String,
    ): RefereeMatchSession?
    suspend fun saveRefereeMatchSession(
        session: RefereeMatchSession,
    )
    suspend fun clearRefereeMatchSession(
        profileId: String,
        matchId: String,
    )
}
