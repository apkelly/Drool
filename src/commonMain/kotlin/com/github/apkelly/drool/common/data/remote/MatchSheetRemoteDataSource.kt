package com.github.apkelly.drool.common.data.remote

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.MatchSheet
import com.github.apkelly.drool.common.domain.model.MatchSheetMember
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
import com.github.apkelly.drool.common.domain.model.TeamSheetStatus

interface MatchSheetRemoteDataSource {
    suspend fun fetchMatchSheet(
        bearerToken: String,
        fixture: Fixture,
    ): MatchSheet

    suspend fun updateMatchSheetMemberAvailability(
        bearerToken: String,
        memberId: String,
        available: Boolean,
    ): MatchSheetMember

    suspend fun updateTeamSheetStatus(
        bearerToken: String,
        matchSheetId: String,
        teamId: String,
        action: TeamSheetAction,
    ): TeamSheetStatus
}
