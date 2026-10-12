package com.github.apkelly.dribl.api

import com.github.apkelly.dribl.dto.ClubListResponse
import com.github.apkelly.dribl.dto.TeamListResponse
import com.github.apkelly.dribl.mapper.toDomain
import com.github.apkelly.dribl.model.Club
import com.github.apkelly.dribl.model.Team
import io.ktor.client.request.get
import io.ktor.client.request.parameter

private const val DISCOVER_PAGE_SIZE = 1_000

internal class DriblDiscoveryApi(
    private val transport: DriblHttpTransport,
) {
    suspend fun fetchClubs(
        bearerToken: String,
    ): List<Club> {
        val clubs = mutableListOf<Club>()
        var page = 1
        var lastPage: Int
        do {
            val response = transport.getBody<ClubListResponse>("/universal/clubs", bearerToken) {
                parameter("page", page)
                parameter("per_page", DISCOVER_PAGE_SIZE)
            }
            val mapped = response.data.mapIndexedNotNull { index, resource ->
                resource.attributes.copy(id = resource.id).toDomain(index)
            }
            if (response.data.isNotEmpty() && mapped.isEmpty()) {
                throw DriblResponseException("Club response contained no valid clubs")
            }
            clubs += mapped
            lastPage = response.meta?.lastPage ?: 1
            page += 1
        } while (page <= lastPage)
        return clubs.distinctBy(Club::id)
    }

    suspend fun fetchTeams(
        bearerToken: String,
        clubId: String?,
    ): List<Team> {
        val teams = mutableListOf<Team>()
        var page = 1
        var lastPage: Int
        do {
            val response = transport.getBody<TeamListResponse>("/universal/teams", bearerToken) {
                clubId?.let {
                    parameter("club_id", it)
                    parameter("current_season", true)
                }
                parameter("page", page)
                parameter("per_page", DISCOVER_PAGE_SIZE)
                parameter("sort", "+club,+age_group,+group")
            }
            val mapped = response.data.mapIndexedNotNull { index, resource ->
                resource.attributes.copy(id = resource.id).toDomain(index)
            }
            if (response.data.isNotEmpty() && mapped.isEmpty()) {
                throw DriblResponseException("Team response contained no valid teams")
            }
            teams += mapped
            lastPage = if (clubId == null) 1 else response.meta?.lastPage ?: 1
            page += 1
        } while (page <= lastPage)
        return teams.distinctBy(Team::id)
    }
}
