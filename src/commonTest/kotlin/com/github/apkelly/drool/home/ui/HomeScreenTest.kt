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

    @Test
    fun upcomingKeepsTodaysMatchesUntilLocalMidnight() {
        val sydney = TimeZone.of("Australia/Sydney")
        val fixtures = listOf(
            fixture("last-week", "2026-10-27T08:00:00Z", FixtureRole.Player),
            fixture("tonight-6pm", "2026-11-03T07:00:00Z", FixtureRole.Referee),
            fixture("tonight-7pm", "2026-11-03T08:00:00Z", FixtureRole.Player),
            fixture("next-week", "2026-11-10T08:00:00Z", FixtureRole.Player),
        )
        // 2026-11-03T19:45:00+11:00 (after both 6pm and 7pm kickoffs, before local midnight)
        val afterKickoffSameNight = Instant.parse("2026-11-03T08:45:00Z").toEpochMilliseconds()
        assertEquals(
            listOf("tonight-6pm", "tonight-7pm"),
            fixtures.upcomingOnNextDay(sydney, nowEpochMillis = afterKickoffSameNight)
                .map(Fixture::id),
        )

        // 2026-11-04T00:00:00+11:00 (local midnight has passed)
        val afterLocalMidnight = Instant.parse("2026-11-03T13:00:00Z").toEpochMilliseconds()
        assertEquals(
            listOf("next-week"),
            fixtures.upcomingOnNextDay(sydney, nowEpochMillis = afterLocalMidnight)
                .map(Fixture::id),
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
