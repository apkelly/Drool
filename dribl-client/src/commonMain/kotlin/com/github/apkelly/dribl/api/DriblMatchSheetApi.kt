package com.github.apkelly.dribl.api

import com.github.apkelly.dribl.dto.MatchSheetMemberAvailabilityRequest
import com.github.apkelly.dribl.dto.MatchSheetMemberResourceDto
import com.github.apkelly.dribl.dto.MatchSheetOfficialResourceDto
import com.github.apkelly.dribl.dto.ResourceList
import com.github.apkelly.dribl.dto.ResourceResponse
import com.github.apkelly.dribl.dto.TeamSheetSubmissionRequest
import com.github.apkelly.dribl.dto.TeamSheetSubmissionResourceDto
import com.github.apkelly.dribl.mapper.toDomain
import com.github.apkelly.dribl.mapper.toTeamSheetStatus
import com.github.apkelly.dribl.model.Fixture
import com.github.apkelly.dribl.model.MatchSheet
import com.github.apkelly.dribl.model.MatchSheetMember
import com.github.apkelly.dribl.model.MatchSheetOfficial
import com.github.apkelly.dribl.model.MatchSheetTeam
import com.github.apkelly.dribl.model.TeamSheetAction
import com.github.apkelly.dribl.model.TeamSheetStatus
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class DriblMatchSheetApi(
    private val transport: DriblHttpTransport,
) {
    suspend fun fetchMatchSheet(
        bearerToken: String,
        fixture: Fixture,
    ): MatchSheet = coroutineScope {
        val matchSheetId = fixture.matchSheetId?.takeIf(String::isNotBlank)
            ?: throw DriblResponseException("Match response did not contain a matchsheet ID")
        val homeTeamId = fixture.homeTeamId?.takeIf(String::isNotBlank)
            ?: throw DriblResponseException("Match response did not contain a home team ID")
        val awayTeamId = fixture.awayTeamId?.takeIf(String::isNotBlank)
            ?: throw DriblResponseException("Match response did not contain an away team ID")
        val homeMembersRequest = async {
            fetchMatchSheetMembers(bearerToken, matchSheetId, homeTeamId)
        }
        val awayMembersRequest = async {
            fetchMatchSheetMembers(bearerToken, matchSheetId, awayTeamId)
        }
        val homeStatusRequest = async {
            fetchTeamSheetStatus(bearerToken, matchSheetId, homeTeamId)
        }
        val awayStatusRequest = async {
            fetchTeamSheetStatus(bearerToken, matchSheetId, awayTeamId)
        }
        val refereesRequest = async {
            fetchMatchSheetOfficials(
                path = "/matchsheet-referees",
                bearerToken = bearerToken,
                fixture = fixture,
                matchSheetId = matchSheetId,
            )
        }
        val officialsRequest = async {
            fetchMatchSheetOfficials(
                path = "/matchsheet-officials",
                bearerToken = bearerToken,
                fixture = fixture,
                matchSheetId = matchSheetId,
            )
        }
        MatchSheet(
            id = matchSheetId,
            status = null,
            homeTeam = MatchSheetTeam(
                id = homeTeamId,
                name = fixture.homeTeamName,
                status = homeStatusRequest.await(),
            ),
            awayTeam = MatchSheetTeam(
                id = awayTeamId,
                name = fixture.awayTeamName,
                status = awayStatusRequest.await(),
            ),
            members = homeMembersRequest.await() + awayMembersRequest.await(),
            referees = refereesRequest.await(),
            officials = officialsRequest.await(),
        )
    }

    suspend fun updateMatchSheetMemberAvailability(
        bearerToken: String,
        memberId: String,
        available: Boolean,
    ): MatchSheetMember {
        val url = transport.url("/matchsheet-members/$memberId")
        val response = transport.client.put(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(
                MatchSheetMemberAvailabilityRequest(
                    available = available,
                    starting = false.takeUnless { available },
                    playing = false.takeUnless { available },
                    isCaptain = false.takeUnless { available },
                    isGoalkeeper = false.takeUnless { available },
                ),
            )
        }.requireBody<ResourceResponse<MatchSheetMemberResourceDto>>(url)
        return response.item.toDomain()
    }

    suspend fun updateTeamSheetStatus(
        bearerToken: String,
        matchSheetId: String,
        teamId: String,
        action: TeamSheetAction,
    ): TeamSheetStatus {
        val url = transport.url("/matchsheet-team-submissions")
        val response = transport.client.patch(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(
                TeamSheetSubmissionRequest(
                    matchSheetId = matchSheetId,
                    teamId = teamId,
                    type = action.name.lowercase(),
                ),
            )
        }.requireBody<ResourceResponse<TeamSheetSubmissionResourceDto>>(url)
        return response.item.attributes.status.toTeamSheetStatus()
    }

    private suspend fun fetchMatchSheetMembers(
        bearerToken: String,
        matchSheetId: String,
        teamId: String,
    ): List<MatchSheetMember> = transport.getBody<ResourceList<MatchSheetMemberResourceDto>>(
        "/matchsheet-members",
        bearerToken,
    ) {
        parameter("matchsheet_id", matchSheetId)
        parameter("team_id", teamId)
    }.items.map(MatchSheetMemberResourceDto::toDomain)

    private suspend fun fetchTeamSheetStatus(
        bearerToken: String,
        matchSheetId: String,
        teamId: String,
    ): TeamSheetStatus = transport.getBody<ResourceResponse<TeamSheetSubmissionResourceDto>>(
        "/matchsheet-team-submissions/$matchSheetId/$teamId",
        bearerToken,
    ).item.attributes.status.toTeamSheetStatus()

    private suspend fun fetchMatchSheetOfficials(
        path: String,
        bearerToken: String,
        fixture: Fixture,
        matchSheetId: String,
    ): List<MatchSheetOfficial> = transport.getBody<ResourceList<MatchSheetOfficialResourceDto>>(
        path,
        bearerToken,
    ) {
        parameter("matchsheet_id", matchSheetId)
        fixture.tenantId?.let { parameter("tenant_id", it) }
        fixture.seasonId?.let { parameter("season_id", it) }
        if (path.endsWith("referees")) parameter("require_user_id", true)
    }.items.map(MatchSheetOfficialResourceDto::toDomain)
}
