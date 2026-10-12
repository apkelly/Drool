package com.github.apkelly.dribl.api

import com.github.apkelly.dribl.model.Fixture
import com.github.apkelly.dribl.model.MatchSheet
import com.github.apkelly.dribl.model.MatchSheetMember
import com.github.apkelly.dribl.model.TeamSheetAction
import com.github.apkelly.dribl.model.TeamSheetStatus

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
