package com.github.apkelly.drool.schedule.ui

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleScreenTest {
    @Test
    fun allocationActionsRequireAssignedRefereeFixture() {
        assertTrue(fixture().hasAssignedRefereeAllocation())
        assertFalse(fixture(role = FixtureRole.Player).hasAssignedRefereeAllocation())
        assertFalse(fixture(allocationId = null).hasAssignedRefereeAllocation())
        assertFalse(fixture(refereeStatus = "accepted").canAcceptRefereeAllocation())
        assertTrue(fixture(refereeStatus = "accepted").canDeclineRefereeAllocation())
        assertFalse(fixture(refereeStatus = "request_contact").canDeclineRefereeAllocation())
    }

    private fun fixture(
        role: FixtureRole = FixtureRole.Referee,
        allocationId: String? = "allocation-1",
        refereeStatus: String? = "pending",
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
        status = FixtureStatus.Scheduled,
        role = role,
        refereeAllocationId = allocationId,
        refereeStatus = refereeStatus,
    )
}
