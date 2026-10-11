package com.github.apkelly.drool.schedule.domain.model

data class RefereeClub(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val timeZone: String,
)

data class RefereeAvailability(
    val id: String,
    val profileId: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val clubId: String?,
    val clubName: String?,
    val recurrence: AvailabilityRecurrence,
    val recurrenceEndEpochMillis: Long?,
)

enum class AvailabilityRecurrence {
    SingleDay,
    Weekly,
    Fortnightly,
    Monthly,
}

data class RefereeAvailabilityOverview(
    val clubs: List<RefereeClub>,
    val availabilities: List<RefereeAvailability>,
)

data class RefereeAvailabilityDraft(
    val startDateEpochMillis: Long,
    val endDateEpochMillis: Long?,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val recurrence: AvailabilityRecurrence,
    val clubId: String,
    val timeZone: String,
)

sealed interface AvailabilityResult<out T> {
    data class Success<T>(
        val value: T,
    ) : AvailabilityResult<T>
    data class Failed(
        val reason: AvailabilityFailure,
    ) : AvailabilityResult<Nothing>
}

enum class AvailabilityFailure {
    Unauthorized,
    Forbidden,
    Offline,
    Server,
    InvalidResponse,
}
