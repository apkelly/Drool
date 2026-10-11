package com.github.apkelly.drool.schedule.data

import com.github.apkelly.dribl.api.DriblHttpException
import com.github.apkelly.dribl.api.DriblResponseException
import com.github.apkelly.dribl.api.DriblTokenStore
import com.github.apkelly.dribl.api.SportsRemoteDataSource
import com.github.apkelly.dribl.model.AvailabilityPeriod
import com.github.apkelly.dribl.model.CreateRefereeAvailabilityRequest
import com.github.apkelly.drool.common.data.time.TimeProvider
import com.github.apkelly.drool.schedule.domain.model.AvailabilityFailure
import com.github.apkelly.drool.schedule.domain.model.AvailabilityRecurrence
import com.github.apkelly.drool.schedule.domain.model.AvailabilityResult
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityDraft
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityOverview
import com.github.apkelly.drool.schedule.domain.repository.AvailabilityRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.time.Instant

class AvailabilityRepositoryImpl(
    private val api: SportsRemoteDataSource,
    private val tokenStore: DriblTokenStore,
    private val timeProvider: TimeProvider,
) : AvailabilityRepository {
    override suspend fun getRefereeClubs(
        profileId: String,
    ) = withProfileToken(profileId) { token ->
        api.fetchActiveRefereeClubs(token).map { it.toDomain() }
    }

    override suspend fun getAvailability(
        profileId: String,
    ) = withProfileToken(profileId) { token ->
        val clubs = api.fetchActiveRefereeClubs(token).map { it.toDomain() }
        RefereeAvailabilityOverview(
            clubs = clubs,
            availabilities = api.fetchRefereeAvailabilities(
                bearerToken = token,
                userId = profileId,
                startDate = availabilityStartDate(
                    epochMillis = timeProvider.nowEpochMillis(),
                    timeZone = if (clubs.isEmpty()) "UTC" else clubs.first().timeZone,
                ),
            )
                .map { it.toDomain(profileId) }
                .sortedBy { it.startEpochMillis },
        )
    }

    private fun availabilityStartDate(
        epochMillis: Long,
        timeZone: String,
    ): String {
        val date = Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.of(timeZone))
            .date
        return "${date}T00:00:00Z"
    }

    override suspend fun createAvailability(
        profileId: String,
        draft: RefereeAvailabilityDraft,
    ) = withProfileToken(profileId) { token ->
        api.createRefereeAvailability(
            token,
            draft.toRequest(profileId, timeProvider.nowEpochMillis()),
        )
        Unit
    }

    override suspend fun deleteAvailability(
        profileId: String,
        availabilityId: String,
    ) = withProfileToken(profileId) { token ->
        api.deleteRefereeAvailability(token, availabilityId)
    }

    private suspend fun <T> withProfileToken(
        profileId: String,
        block: suspend (String) -> T,
    ): AvailabilityResult<T> {
        val rootToken = tokenStore.getBearerToken()
            ?: return AvailabilityResult.Failed(AvailabilityFailure.Unauthorized)
        return availabilityResult {
            val profileToken = api.createProfileSession(rootToken, profileId)
            block(profileToken)
        }
    }
}

private fun com.github.apkelly.dribl.model.RefereeClub.toDomain() =
    com.github.apkelly.drool.schedule.domain.model.RefereeClub(
        id = id,
        name = name,
        logoUrl = logoUrl,
        timeZone = timeZone,
    )

private fun com.github.apkelly.dribl.model.RefereeAvailability.toDomain(
    profileId: String,
) = com.github.apkelly.drool.schedule.domain.model.RefereeAvailability(
    id = id,
    profileId = profileId,
    startEpochMillis = startEpochMillis,
    endEpochMillis = endEpochMillis,
    clubId = clubId,
    clubName = clubName,
    recurrence = when (period) {
        AvailabilityPeriod.SingleDay -> AvailabilityRecurrence.SingleDay
        AvailabilityPeriod.Weekly -> AvailabilityRecurrence.Weekly
        AvailabilityPeriod.Fortnightly -> AvailabilityRecurrence.Fortnightly
        AvailabilityPeriod.Monthly -> AvailabilityRecurrence.Monthly
    },
    recurrenceEndEpochMillis = templateEndEpochMillis,
)

private fun RefereeAvailabilityDraft.toRequest(
    profileId: String,
    nowEpochMillis: Long,
): CreateRefereeAvailabilityRequest {
    val zone = TimeZone.of(timeZone)
    val startDate = Instant.fromEpochMilliseconds(startDateEpochMillis)
        .toLocalDateTime(TimeZone.UTC)
        .date
    val today = Instant.fromEpochMilliseconds(nowEpochMillis)
        .toLocalDateTime(zone)
        .date
    require(startDate >= today) {
        "Availability cannot start in the past"
    }
    val finalDate = Instant.fromEpochMilliseconds(
        if (recurrence != AvailabilityRecurrence.SingleDay) {
            requireNotNull(endDateEpochMillis)
        } else {
            startDateEpochMillis
        },
    ).toLocalDateTime(TimeZone.UTC).date
    require(finalDate <= today.plus(MAX_AVAILABILITY_MONTHS, DateTimeUnit.MONTH)) {
        "Availability cannot extend more than six months into the future"
    }
    val start = LocalDateTime(startDate, LocalTime(startHour, startMinute))
    val end = LocalDateTime(finalDate, LocalTime(endHour, endMinute))
    require(end.toInstant(zone) > start.toInstant(zone)) {
        "Availability end must be after its start"
    }
    return CreateRefereeAvailabilityRequest(
        userId = profileId,
        start = start.toInstant(zone).toString(),
        end = end.toInstant(zone).toString(),
        startTime = localTime(startHour, startMinute),
        endTime = localTime(endHour, endMinute),
        allClubs = false,
        clubId = clubId,
        period = recurrence.name,
        day = if (recurrence == AvailabilityRecurrence.SingleDay) {
            null
        } else {
            (startDate.dayOfWeek.ordinal + 1) % 7
        },
        timeZone = timeZone,
    )
}

private fun localTime(
    hour: Int,
    minute: Int,
): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

private suspend fun <T> availabilityResult(
    block: suspend () -> T,
): AvailabilityResult<T> = try {
    AvailabilityResult.Success(block())
} catch (error: DriblHttpException) {
    AvailabilityResult.Failed(
        when (error.statusCode) {
            HTTP_UNAUTHORIZED -> AvailabilityFailure.Unauthorized
            HTTP_FORBIDDEN -> AvailabilityFailure.Forbidden
            in 500..599 -> AvailabilityFailure.Server
            else -> AvailabilityFailure.InvalidResponse
        },
    )
} catch (_: DriblResponseException) {
    AvailabilityResult.Failed(AvailabilityFailure.InvalidResponse)
} catch (_: SerializationException) {
    AvailabilityResult.Failed(AvailabilityFailure.InvalidResponse)
} catch (_: IllegalArgumentException) {
    AvailabilityResult.Failed(AvailabilityFailure.InvalidResponse)
} catch (_: IOException) {
    AvailabilityResult.Failed(AvailabilityFailure.Offline)
}

private const val MAX_AVAILABILITY_MONTHS = 6
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
