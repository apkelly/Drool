package com.github.apkelly.drool.schedule.ui

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import com.github.apkelly.drool.common.ui.widgets.canAcceptRefereeAllocation
import com.github.apkelly.drool.common.ui.widgets.canDeclineRefereeAllocation
import com.github.apkelly.drool.common.ui.widgets.canDeclineRefereeAllocationFromCard
import com.github.apkelly.drool.common.ui.widgets.hasAssignedRefereeAllocation
import com.github.apkelly.drool.common.ui.widgets.mergeRefereeAllocationFrom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleScreenTest {
    @Test
    fun allocationActionsRequireAssignedRefereeFixture() {
        assertTrue(fixture().hasAssignedRefereeAllocation())
        assertFalse(fixture(role = FixtureRole.Player).hasAssignedRefereeAllocation())
        assertFalse(fixture(allocationId = null).hasAssignedRefereeAllocation())
        assertTrue(fixture(refereeStatus = "pending").canAcceptRefereeAllocation())
        assertTrue(fixture(refereeStatus = "pending").canDeclineRefereeAllocationFromCard())
        assertFalse(fixture(refereeStatus = "accepted").canAcceptRefereeAllocation())
        assertFalse(fixture(refereeStatus = "accepted").canDeclineRefereeAllocationFromCard())
        assertTrue(fixture(refereeStatus = "accepted").canDeclineRefereeAllocation())
        assertFalse(fixture(refereeStatus = "request_contact").canDeclineRefereeAllocation())
        assertFalse(
            fixture(
                refereeStatus = "accepted",
                status = FixtureStatus.Completed,
            ).canDeclineRefereeAllocation(),
        )
        assertFalse(
            fixture(
                refereeStatus = "pending",
                status = FixtureStatus.Completed,
            ).canAcceptRefereeAllocation(),
        )
    }

    @Test
    fun mergeRefereeAllocationPreservesRefereeDetails() {
        val matchDetailsFixture = fixture(
            role = FixtureRole.Player,
            allocationId = null,
            refereeStatus = null,
        ).copy(refereeRole = null)
        val scheduleRefereeFixture = fixture(
            role = FixtureRole.Referee,
            allocationId = "alloc-27",
            refereeStatus = "pending",
        ).copy(refereeRole = "Assistant Referee 1")

        val merged = matchDetailsFixture.mergeRefereeAllocationFrom(scheduleRefereeFixture)
        assertEquals(FixtureRole.Referee, merged.role)
        assertEquals("alloc-27", merged.refereeAllocationId)
        assertEquals("Assistant Referee 1", merged.refereeRole)
        assertEquals("pending", merged.refereeStatus)
    }

    private fun fixture(
        role: FixtureRole = FixtureRole.Referee,
        allocationId: String? = "allocation-1",
        refereeStatus: String? = "pending",
        status: FixtureStatus = FixtureStatus.Scheduled,
    ) = Fixture(
        id = "match-1",
        kickoffEpochMillis = 1L,
        homeTeamId = "home",
        homeTeamName = "Home",
        awayTeamId = "away",
        awayTeamName = "Away",
        competitionName = null,
        venueName = null,
        userTeamId = null,
        status = status,
        role = role,
        refereeAllocationId = allocationId,
        refereeStatus = refereeStatus,
    )
}
