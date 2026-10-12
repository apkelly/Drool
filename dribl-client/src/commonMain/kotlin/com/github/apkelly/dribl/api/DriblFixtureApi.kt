package com.github.apkelly.dribl.api

import com.github.apkelly.dribl.dto.LadderDetailResponse
import com.github.apkelly.dribl.dto.LadderListResponse
import com.github.apkelly.dribl.dto.MatchListResponse
import com.github.apkelly.dribl.dto.MatchResourceDto
import com.github.apkelly.dribl.dto.MatchResponse
import com.github.apkelly.dribl.mapper.fields
import com.github.apkelly.dribl.mapper.ladderEntryFromDto
import com.github.apkelly.dribl.mapper.teamIdentifier
import com.github.apkelly.dribl.mapper.toDomain
import com.github.apkelly.dribl.model.Fixture
import com.github.apkelly.dribl.model.FixtureStatus
import com.github.apkelly.dribl.model.LadderEntry
import com.github.apkelly.dribl.model.TeamHub
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Instant

internal class DriblFixtureApi(
    private val transport: DriblHttpTransport,
    private val currentEpochMillis: () -> Long,
) {
    suspend fun fetchFixtures(
        bearerToken: String,
        userId: String?,
    ): List<Fixture> {
        val response = transport.getBody<MatchListResponse>("/universal/matches", bearerToken) {
            userId?.let { parameter("user_id", it) }
            parameter("start_date", upcomingScheduleStart(currentEpochMillis()))
            parameter("remove_byes", true)
            parameter("require_adherance", true)
            parameter("sort", "+date,+home_team")
        }
        return response.data.map(MatchResourceDto::toDomain)
    }

    suspend fun fetchTeamHub(
        bearerToken: String,
        teamId: String,
        competitionName: String?,
        leagueId: String?,
    ): TeamHub = coroutineScope {
        val matchesRequest = async {
            transport.getBody<MatchListResponse>(
                "/universal/matches",
                bearerToken,
            ) {
                leagueId?.let { parameter("league_id", it) }
                parameter("remove_byes", true)
                parameter("require_adherance", true)
                parameter("sort", "+date,+home_team")
            }
        }
        val laddersRequest = async {
            transport.getBody<LadderListResponse>("/universal/ladders", bearerToken)
        }
        var fixtures = matchesRequest.await().data
            .map(MatchResourceDto::toDomain)
            .filter { it.involves(teamId) }

        val laddersResponse = laddersRequest.await()
        val availableLadders = laddersResponse.ladders.ifEmpty { laddersResponse.data }
        val (preferredLadders, remainingLadders) = availableLadders.partition {
            it.name?.trim().equals(competitionName?.trim(), ignoreCase = true)
        }
        val ladders = preferredLadders + remainingLadders
        var selectedName: String? = null
        var selectedEntries = emptyList<LadderEntry>()
        val identifiedLadders = ladders.mapNotNull { ladder ->
            ladder.id.textValue?.let { ladderId -> ladder to ladderId }
        }
        for ((ladder, ladderId) in identifiedLadders) {
            val detail = transport.getBody<LadderDetailResponse>(
                "/universal/ladders/$ladderId",
                bearerToken,
            ) {
                parameter("require_pools", true)
                parameter("require_form", true)
                parameter("require_adjustments", true)
                parameter("require_championship", true)
            }
            val entries = detail.ladderEntries
                .mapIndexedNotNull { index, entry ->
                    ladderEntryFromDto(index, entry, ladder.name)
                }
            if (entries.any { it.teamId == teamId }) {
                selectedName = ladder.name
                selectedEntries = entries
                val selected = detail.ladderEntries.firstOrNull {
                    it.fields().teamIdentifier == teamId
                }?.fields()
                val formFixtures = (
                    selected?.upcomingMatches.orEmpty() +
                        selected?.recentMatches.orEmpty()
                    ).mapIndexedNotNull { index, fixture ->
                    fixture.toDomain(index, competitionName = ladder.name)
                }
                fixtures = (fixtures + formFixtures)
                    .filter { it.involves(teamId) }
                    .distinctBy(Fixture::id)
                break
            }
        }
        val (results, matches) = fixtures.partition {
            it.status == FixtureStatus.Completed ||
                (it.homeScore != null && it.awayScore != null)
        }
        TeamHub(
            teamId = teamId,
            matches = matches.sortedBy(Fixture::kickoffEpochMillis),
            results = results.sortedByDescending(Fixture::kickoffEpochMillis),
            ladderName = selectedName,
            ladder = selectedEntries,
        )
    }

    suspend fun fetchMatchDetails(
        bearerToken: String,
        matchId: String,
    ): Fixture {
        val response = transport.getBody<MatchResponse>(
            "/universal/matches/$matchId",
            bearerToken,
        ) {
            parameter("require_season", true)
            parameter("require_adherance", true)
        }
        return response.data.toDomain()
    }
}

private fun upcomingScheduleStart(
    currentEpochMillis: Long,
): String = Instant.fromEpochMilliseconds(currentEpochMillis - SCHEDULE_LOOKBACK_MILLIS).toString()

private fun Fixture.involves(
    teamId: String,
): Boolean = homeTeamId == teamId || awayTeamId == teamId || userTeamId == teamId

private val JsonPrimitive?.textValue: String?
    get() = this?.contentOrNull?.takeUnless { it.isBlank() || it == "null" }

private const val SCHEDULE_LOOKBACK_MILLIS = 14L * 24L * 60L * 60L * 1_000L
