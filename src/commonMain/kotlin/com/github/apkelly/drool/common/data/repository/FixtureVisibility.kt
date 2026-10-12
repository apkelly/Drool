package com.github.apkelly.drool.common.data.repository

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureStatus

/**
 * Keeps fixtures that should still appear in an upcoming schedule.
 *
 * Live fixtures remain visible after their nominal kickoff, while terminal fixtures are removed.
 * Unknown non-terminal statuses receive a grace period because Dribl can update status late.
 */
internal fun List<Fixture>.upcomingAt(
    nowEpochMillis: Long,
): List<Fixture> = filter { fixture ->
    when {
        fixture.kickoffEpochMillis >= nowEpochMillis -> true

        fixture.status == FixtureStatus.Live -> true

        fixture.status in finishedFixtureStatuses -> false

        else ->
            fixture.kickoffEpochMillis + MAXIMUM_FIXTURE_VISIBILITY_MILLIS >
                nowEpochMillis
    }
}

/**
 * Keeps upcoming fixtures and enough recent history to revisit match-day workflows.
 */
internal fun List<Fixture>.recentAndUpcomingAt(
    nowEpochMillis: Long,
): List<Fixture> = filter { fixture ->
    fixture.status == FixtureStatus.Live ||
        fixture.kickoffEpochMillis >= nowEpochMillis - SCHEDULE_HISTORY_MILLIS
}

private val finishedFixtureStatuses = setOf(
    FixtureStatus.Completed,
    FixtureStatus.Cancelled,
    FixtureStatus.Washout,
)
private const val MAXIMUM_FIXTURE_VISIBILITY_MILLIS = 3L * 60L * 60L * 1_000L
private const val SCHEDULE_HISTORY_MILLIS = 14L * 24L * 60L * 60L * 1_000L
