package com.github.apkelly.drool.home.ui

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class HomeScreenTest {
    @Test
    fun upcomingIncludesEveryEventOnEarliestLocalDay() {
        val fixtures = listOf(
            fixture("afternoon-player", "2026-10-13T04:00:00Z", FixtureRole.Player),
            fixture("later-day", "2026-10-14T00:00:00Z", FixtureRole.Player),
            fixture("morning-referee", "2026-10-12T22:00:00Z", FixtureRole.Referee),
        )

        assertEquals(
            listOf("morning-referee", "afternoon-player"),
            fixtures.upcomingOnNextDay(TimeZone.of("Australia/Sydney")).map(Fixture::id),
        )
    }

    @Test
    fun upcomingUsesLocalDateAcrossUtcBoundary() {
        val fixtures = listOf(
            fixture("late-sydney", "2026-10-13T12:30:00Z", FixtureRole.Player),
            fixture("early-sydney", "2026-10-13T13:30:00Z", FixtureRole.Referee),
        )

        assertEquals(
            listOf("late-sydney"),
            fixtures.upcomingOnNextDay(TimeZone.of("Australia/Sydney")).map(Fixture::id),
        )
    }

    private fun fixture(
        id: String,
        kickoff: String,
        role: FixtureRole,
    ) = Fixture(
        id = id,
        kickoffEpochMillis = Instant.parse(kickoff).toEpochMilliseconds(),
        homeTeamId = null,
        homeTeamName = "Home",
        awayTeamId = null,
        awayTeamName = "Away",
        competitionName = null,
        venueName = null,
        userTeamId = null,
        status = FixtureStatus.Scheduled,
        role = role,
    )
}
