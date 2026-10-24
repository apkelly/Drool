package com.github.apkelly.drool.match.domain.usecase

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.MatchSheetGoalDraft
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
import com.github.apkelly.drool.match.domain.model.RefereeMatchSession
import com.github.apkelly.drool.match.domain.repository.MatchRepository

class LoadMatchDetailsUseCase(
    private val repository: MatchRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        matchId: String,
    ) = repository.loadMatchDetails(profileId, matchId)
}

class LoadMatchWeatherUseCase(
    private val repository: MatchRepository,
) {
    suspend operator fun invoke(
        fixture: Fixture,
    ) = invoke(fixture, force = false)

    suspend operator fun invoke(
        fixture: Fixture,
        force: Boolean,
    ) = repository.loadMatchWeather(fixture, force)
}

class LoadMatchSheetUseCase(
    private val repository: MatchRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        fixture: Fixture,
    ) = repository.loadMatchSheet(profileId, fixture)
}

class UpdateMatchSheetMemberUseCase(
    private val repository: MatchRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        memberId: String,
        available: Boolean,
    ) = repository.updateMemberAvailability(profileId, memberId, available)
}

class UpdateTeamSheetStatusUseCase(
    private val repository: MatchRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        matchSheetId: String,
        teamId: String,
        action: TeamSheetAction,
    ) = repository.updateTeamSheetStatus(
        profileId = profileId,
        matchSheetId = matchSheetId,
        teamId = teamId,
        action = action,
    )
}

class CheckInRefereeUseCase(
    private val repository: MatchRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        refereeOfficialId: String,
        checkedIn: Boolean = true,
    ) = repository.updateRefereeCheckIn(
        profileId = profileId,
        refereeOfficialId = refereeOfficialId,
        checkedIn = checkedIn,
    )
}

@Suppress("LongParameterList")
class SubmitRefereeMatchReportUseCase(
    private val repository: MatchRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        fixture: Fixture,
        scoreId: String?,
        homeHalfTime: Int?,
        awayHalfTime: Int?,
        homeFullTime: Int,
        awayFullTime: Int,
        goals: List<MatchSheetGoalDraft>?,
        existingGoalIds: List<String>,
    ) = repository.submitRefereeMatchReport(
        profileId = profileId,
        fixture = fixture,
        scoreId = scoreId,
        homeHalfTime = homeHalfTime,
        awayHalfTime = awayHalfTime,
        homeFullTime = homeFullTime,
        awayFullTime = awayFullTime,
        goals = goals,
        existingGoalIds = existingGoalIds,
    )
}

class RefereeMatchSessionUseCase(
    private val repository: MatchRepository,
) {
    suspend fun load(
        profileId: String,
        matchId: String,
    ): RefereeMatchSession? = repository.loadRefereeMatchSession(profileId, matchId)

    suspend fun save(
        session: RefereeMatchSession,
    ) = repository.saveRefereeMatchSession(session)

    suspend fun clear(
        profileId: String,
        matchId: String,
    ) = repository.clearRefereeMatchSession(profileId, matchId)
}
