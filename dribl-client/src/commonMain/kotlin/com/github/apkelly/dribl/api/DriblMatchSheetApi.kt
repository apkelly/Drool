package com.github.apkelly.dribl.api

import com.github.apkelly.dribl.dto.MatchSheetGoalRequest
import com.github.apkelly.dribl.dto.MatchSheetGoalResourceDto
import com.github.apkelly.dribl.dto.MatchSheetMemberAvailabilityRequest
import com.github.apkelly.dribl.dto.MatchSheetMemberResourceDto
import com.github.apkelly.dribl.dto.MatchSheetOfficialResourceDto
import com.github.apkelly.dribl.dto.MatchSheetScoreRequest
import com.github.apkelly.dribl.dto.MatchSheetScoreResourceDto
import com.github.apkelly.dribl.dto.RefereePresenceUpdateRequest
import com.github.apkelly.dribl.dto.ResourceList
import com.github.apkelly.dribl.dto.ResourceResponse
import com.github.apkelly.dribl.dto.TeamDto
import com.github.apkelly.dribl.dto.TeamResourceDto
import com.github.apkelly.dribl.dto.TeamSheetSubmissionRequest
import com.github.apkelly.dribl.dto.TeamSheetSubmissionResourceDto
import com.github.apkelly.dribl.mapper.toDomain
import com.github.apkelly.dribl.mapper.toTeamSheetStatus
import com.github.apkelly.dribl.model.Fixture
import com.github.apkelly.dribl.model.MatchSheet
import com.github.apkelly.dribl.model.MatchSheetGoal
import com.github.apkelly.dribl.model.MatchSheetGoalDraft
import com.github.apkelly.dribl.model.MatchSheetMember
import com.github.apkelly.dribl.model.MatchSheetOfficial
import com.github.apkelly.dribl.model.MatchSheetScore
import com.github.apkelly.dribl.model.MatchSheetTeam
import com.github.apkelly.dribl.model.TeamSheetAction
import com.github.apkelly.dribl.model.TeamSheetStatus
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
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
        val scoreRequest = async {
            fetchMatchSheetScoreOrNull(bearerToken, fixture, matchSheetId)
        }
        val goalsRequest = async {
            fetchMatchSheetGoalsOrEmpty(bearerToken, fixture, matchSheetId)
        }
        val homeTeamAttributesRequest = async {
            fetchTeamAttributesOrNull(bearerToken, fixture, homeTeamId)
        }
        val awayTeamAttributesRequest = async {
            fetchTeamAttributesOrNull(bearerToken, fixture, awayTeamId)
        }
        val homeTeamAttributes = homeTeamAttributesRequest.await()
        val awayTeamAttributes = awayTeamAttributesRequest.await()
        MatchSheet(
            id = matchSheetId,
            status = null,
            homeTeam = MatchSheetTeam(
                id = homeTeamId,
                name = fixture.homeTeamName,
                status = homeStatusRequest.await(),
                primaryColor = homeTeamAttributes.normalizedPrimaryColor(),
                secondaryColor = homeTeamAttributes.normalizedSecondaryColor(),
            ),
            awayTeam = MatchSheetTeam(
                id = awayTeamId,
                name = fixture.awayTeamName,
                status = awayStatusRequest.await(),
                primaryColor = awayTeamAttributes.normalizedPrimaryColor(),
                secondaryColor = awayTeamAttributes.normalizedSecondaryColor(),
            ),
            members = homeMembersRequest.await() + awayMembersRequest.await(),
            referees = refereesRequest.await(),
            officials = officialsRequest.await(),
            score = scoreRequest.await(),
            goals = goalsRequest.await(),
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

    suspend fun updateRefereePresence(
        bearerToken: String,
        refereeOfficialId: String,
        checkedIn: Boolean,
    ) {
        val url = transport.url("/referee-mapper/$refereeOfficialId")
        transport.client.put(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(
                RefereePresenceUpdateRequest(
                    presence = if (checkedIn) "present" else "no-show",
                ),
            )
        }.requireSuccess(url)
    }

    suspend fun submitMatchSheetScore(
        bearerToken: String,
        matchSheetId: String,
        scoreId: String?,
        homeHalfTime: Int?,
        awayHalfTime: Int?,
        homeFullTime: Int,
        awayFullTime: Int,
    ): MatchSheetScore {
        val payload = MatchSheetScoreRequest(
            matchSheetId = matchSheetId,
            homeHalfTime = homeHalfTime,
            awayHalfTime = awayHalfTime,
            homeFullTime = homeFullTime,
            awayFullTime = awayFullTime,
        )
        val cleanScoreId = scoreId?.takeIf(String::isNotBlank)
        val response = if (cleanScoreId != null) {
            val url = transport.url("/matchsheet-scores/$cleanScoreId")
            transport.client.put(url) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
                bearerAuth(bearerToken)
                setBody(payload)
            }.requireBody<ResourceResponse<MatchSheetScoreResourceDto>>(url)
        } else {
            val url = transport.url("/matchsheet-scores")
            transport.client.post(url) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
                bearerAuth(bearerToken)
                setBody(payload)
            }.requireBody<ResourceResponse<MatchSheetScoreResourceDto>>(url)
        }
        return response.item.toDomain()
    }

    suspend fun createMatchSheetGoal(
        bearerToken: String,
        matchSheetId: String,
        draft: MatchSheetGoalDraft,
    ): MatchSheetGoal {
        val url = transport.url("/matchsheet-goals")
        val response = transport.client.post(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(
                MatchSheetGoalRequest(
                    matchSheetId = matchSheetId,
                    matchSheetMemberId = draft.matchSheetMemberId,
                    gameSection = draft.half.apiValue,
                    minute = draft.minute,
                    ownGoal = draft.ownGoal,
                    penaltyKick = draft.penaltyKick,
                ),
            )
        }.requireBody<ResourceResponse<MatchSheetGoalResourceDto>>(url)
        return response.item.toDomain()
    }

    suspend fun deleteMatchSheetGoal(
        bearerToken: String,
        goalId: String,
    ) {
        val url = transport.url("/matchsheet-goals/$goalId")
        transport.client.delete(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
        }.requireSuccess(url)
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

    private suspend fun fetchMatchSheetScoreOrNull(
        bearerToken: String,
        fixture: Fixture,
        matchSheetId: String,
    ): MatchSheetScore? = runCatching {
        transport.getBody<ResourceList<MatchSheetScoreResourceDto>>(
            "/matchsheet-scores",
            bearerToken,
        ) {
            parameter("matchsheet_id", matchSheetId)
            fixture.tenantId?.let { parameter("tenant_id", it) }
            fixture.seasonId?.let { parameter("season_id", it) }
        }.items.firstOrNull()?.toDomain()
    }.onFailure { error ->
        if (error is CancellationException) throw error
    }.getOrNull()

    private suspend fun fetchMatchSheetGoalsOrEmpty(
        bearerToken: String,
        fixture: Fixture,
        matchSheetId: String,
    ): List<MatchSheetGoal> = runCatching {
        transport.getBody<ResourceList<MatchSheetGoalResourceDto>>(
            "/matchsheet-goals",
            bearerToken,
        ) {
            parameter("matchsheet_id", matchSheetId)
            fixture.tenantId?.let { parameter("tenant_id", it) }
            fixture.seasonId?.let { parameter("season_id", it) }
        }.items.map(MatchSheetGoalResourceDto::toDomain)
    }.onFailure { error ->
        if (error is CancellationException) throw error
    }.getOrDefault(emptyList())

    private suspend fun fetchTeamAttributesOrNull(
        bearerToken: String,
        fixture: Fixture,
        teamId: String,
    ): TeamDto? = runCatching {
        transport.getBody<ResourceResponse<TeamResourceDto>>(
            "/universal/teams/$teamId",
            bearerToken,
        ) {
            fixture.tenantId?.let { parameter("tenant_id", it) }
            fixture.seasonId?.let { parameter("season_id", it) }
        }.item.attributes
    }.onFailure { error ->
        if (error is CancellationException) throw error
    }.getOrNull()
}

private fun TeamDto?.normalizedPrimaryColor(): String? =
    (this?.primaryColor ?: this?.color)?.trim()?.takeIf(String::isNotEmpty)

private fun TeamDto?.normalizedSecondaryColor(): String? =
    (this?.secondaryColor ?: this?.accent)?.trim()?.takeIf(String::isNotEmpty)
