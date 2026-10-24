package com.github.apkelly.drool.match.data

import com.github.apkelly.drool.common.data.remote.SportsRemoteDataSource
import com.github.apkelly.drool.common.data.repository.operationResult
import com.github.apkelly.drool.common.data.repository.profileToken
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.MatchSheetGoalDraft
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
import com.github.apkelly.drool.match.data.weather.MatchWeatherDataSource
import com.github.apkelly.drool.match.domain.model.RefereeMatchSession
import com.github.apkelly.drool.match.domain.repository.MatchRepository

class MatchRepositoryImpl(
    private val api: SportsRemoteDataSource,
    private val store: BearerTokenStore,
    private val weatherDataSource: MatchWeatherDataSource,
) : MatchRepository {
    override suspend fun loadMatchDetails(
        profileId: String,
        matchId: String,
    ) = operationResult {
        api.fetchMatchDetails(
            profileToken(api, store, profileId),
            matchId,
        ).copy(profileId = profileId)
    }

    override suspend fun loadMatchWeather(
        fixture: Fixture,
        force: Boolean,
    ) = operationResult {
        weatherDataSource.loadMatchWeather(fixture, force)
    }

    override suspend fun loadMatchSheet(
        profileId: String,
        fixture: Fixture,
    ) = operationResult {
        api.fetchMatchSheet(
            profileToken(api, store, profileId),
            fixture,
        )
    }

    override suspend fun updateMemberAvailability(
        profileId: String,
        memberId: String,
        available: Boolean,
    ) = operationResult {
        api.updateMatchSheetMemberAvailability(
            profileToken(api, store, profileId),
            memberId,
            available,
        )
    }

    override suspend fun updateTeamSheetStatus(
        profileId: String,
        matchSheetId: String,
        teamId: String,
        action: TeamSheetAction,
    ) = operationResult {
        api.updateTeamSheetStatus(
            bearerToken = profileToken(api, store, profileId),
            matchSheetId = matchSheetId,
            teamId = teamId,
            action = action,
        )
    }

    override suspend fun updateRefereeCheckIn(
        profileId: String,
        refereeOfficialId: String,
        checkedIn: Boolean,
    ) = operationResult {
        api.updateRefereePresence(
            bearerToken = profileToken(api, store, profileId),
            refereeOfficialId = refereeOfficialId,
            checkedIn = checkedIn,
        )
    }

    override suspend fun submitRefereeMatchReport(
        profileId: String,
        fixture: Fixture,
        scoreId: String?,
        homeHalfTime: Int?,
        awayHalfTime: Int?,
        homeFullTime: Int,
        awayFullTime: Int,
        goals: List<MatchSheetGoalDraft>?,
        existingGoalIds: List<String>,
    ) = operationResult {
        val matchSheetId = fixture.matchSheetId?.takeIf(String::isNotBlank)
            ?: error("Match response did not contain a matchsheet ID")
        val bearerToken = profileToken(api, store, profileId)
        val submittedScore = api.submitMatchSheetScore(
            bearerToken = bearerToken,
            matchSheetId = matchSheetId,
            scoreId = scoreId,
            homeHalfTime = homeHalfTime,
            awayHalfTime = awayHalfTime,
            homeFullTime = homeFullTime,
            awayFullTime = awayFullTime,
        )
        val submittedGoals = if (goals != null) {
            existingGoalIds.forEach { goalId ->
                api.deleteMatchSheetGoal(
                    bearerToken = bearerToken,
                    goalId = goalId,
                )
            }
            goals.map { draft ->
                api.createMatchSheetGoal(
                    bearerToken = bearerToken,
                    matchSheetId = matchSheetId,
                    draft = draft,
                )
            }
        } else {
            null
        }
        val refreshedSheet = api.fetchMatchSheet(bearerToken, fixture)
        refreshedSheet.copy(
            score = refreshedSheet.score ?: submittedScore,
            goals = submittedGoals ?: refreshedSheet.goals,
        )
    }

    override suspend fun loadRefereeMatchSession(
        profileId: String,
        matchId: String,
    ): RefereeMatchSession? = store.getRefereeMatchSession(profileId, matchId)

    override suspend fun saveRefereeMatchSession(
        session: RefereeMatchSession,
    ) = store.saveRefereeMatchSession(session)

    override suspend fun clearRefereeMatchSession(
        profileId: String,
        matchId: String,
    ) = store.clearRefereeMatchSession(profileId, matchId)
}
