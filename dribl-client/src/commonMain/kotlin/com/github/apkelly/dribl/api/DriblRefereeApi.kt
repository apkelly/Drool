package com.github.apkelly.dribl.api

import com.github.apkelly.dribl.dto.ClubResourceDto
import com.github.apkelly.dribl.dto.CreateRefereeAvailabilityRequest
import com.github.apkelly.dribl.dto.RefereeAvailabilityListResponse
import com.github.apkelly.dribl.dto.RefereeAvailabilityResourceDto
import com.github.apkelly.dribl.dto.RefereeAvailabilityResponse
import com.github.apkelly.dribl.dto.ResourceList
import com.github.apkelly.dribl.dto.ScheduleAllocationResourceDto
import com.github.apkelly.dribl.dto.UpdateRefereeAllocationRequest
import com.github.apkelly.dribl.mapper.toDomain
import com.github.apkelly.dribl.model.Fixture
import com.github.apkelly.dribl.model.RefereeAvailability
import com.github.apkelly.dribl.model.RefereeClub
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Instant

internal class DriblRefereeApi(
    private val transport: DriblHttpTransport,
    private val currentEpochMillis: () -> Long,
) {
    suspend fun fetchRefereeFixtures(
        bearerToken: String,
        userId: String,
    ): List<Fixture> {
        val response = transport.getBody<ResourceList<ScheduleAllocationResourceDto>>(
            "/universal/allocations/",
            bearerToken,
        ) {
            parameter("start_date", upcomingScheduleStart(currentEpochMillis()))
            parameter("end_date", refereeScheduleEnd(currentEpochMillis()))
            parameter("user_id", userId)
        }
        val fixtures = response.items.mapNotNull(ScheduleAllocationResourceDto::toDomain)
        if (response.items.isNotEmpty() && fixtures.isEmpty()) {
            throw DriblResponseException("Allocation response contained no valid fixtures")
        }
        return fixtures
    }

    suspend fun updateRefereeAllocation(
        bearerToken: String,
        allocationId: String,
        status: String,
    ) {
        val url = transport.url("/universal/allocations/$allocationId")
        transport.client.patch(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(UpdateRefereeAllocationRequest(status))
        }.requireSuccess(url)
    }

    suspend fun fetchActiveRefereeClubs(
        bearerToken: String,
    ): List<RefereeClub> {
        val response = transport.getBody<ResourceList<ClubResourceDto>>(
            "/universal/active-referee-clubs",
            bearerToken,
        )
        return response.items.mapNotNull { resource ->
            val club = resource.attributes
            val id = (club.id ?: club.clubId ?: resource.id).textValue
                ?: return@mapNotNull null
            val name = club.name ?: club.clubName ?: club.code
                ?: return@mapNotNull null
            RefereeClub(
                id = id,
                name = name,
                logoUrl = club.logoUrl ?: club.image,
                timeZone = club.settings?.timeZone ?: "Australia/Sydney",
            )
        }.distinctBy(RefereeClub::id)
    }

    suspend fun fetchRefereeAvailabilities(
        bearerToken: String,
        userId: String,
        startDate: String,
    ): List<RefereeAvailability> = transport.getBody<RefereeAvailabilityListResponse>(
        "/universal/availabilities",
        bearerToken,
    ) {
        parameter("start_date", startDate)
        parameter("direction", "asc")
        parameter("user_id", userId)
    }.data.mapNotNull(RefereeAvailabilityResourceDto::toDomain)

    suspend fun createRefereeAvailability(
        bearerToken: String,
        request: CreateRefereeAvailabilityRequest,
    ): RefereeAvailability {
        val endpoint = "/universal/availabilities"
        val url = transport.url(endpoint)
        val response = transport.client.post(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(request)
        }.requireBody<RefereeAvailabilityResponse>(url)
        return response.data.toDomain()
            ?: throw DriblResponseException(
                "Availability response did not contain valid dates",
            )
    }

    suspend fun deleteRefereeAvailability(
        bearerToken: String,
        availabilityId: String,
    ) {
        val url = transport.url("/universal/availabilities/$availabilityId")
        transport.client.delete(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
        }.requireSuccess(url)
    }
}

private fun upcomingScheduleStart(
    currentEpochMillis: Long,
): String = Instant.fromEpochMilliseconds(currentEpochMillis - SCHEDULE_LOOKBACK_MILLIS).toString()

private fun refereeScheduleEnd(
    currentEpochMillis: Long,
): String = Instant.fromEpochMilliseconds(
    currentEpochMillis + REFEREE_SCHEDULE_LOOKAHEAD_MILLIS,
).toString()

private val JsonPrimitive?.textValue: String?
    get() = this?.contentOrNull?.takeUnless { it.isBlank() || it == "null" }

private const val SCHEDULE_LOOKBACK_MILLIS = 14L * 24L * 60L * 60L * 1_000L
private const val REFEREE_SCHEDULE_LOOKAHEAD_MILLIS = 90L * 24L * 60L * 60L * 1_000L
