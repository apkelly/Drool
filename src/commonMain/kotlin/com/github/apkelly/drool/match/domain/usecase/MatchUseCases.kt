package com.github.apkelly.drool.match.domain.usecase

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
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
