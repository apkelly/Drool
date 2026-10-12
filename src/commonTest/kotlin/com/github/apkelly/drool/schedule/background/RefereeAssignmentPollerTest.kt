package com.github.apkelly.drool.schedule.background

import com.github.apkelly.drool.common.data.local.entity.RefereeAssignmentSnapshotEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefereeAssignmentPollerTest {
    @Test
    fun initialBaselineDoesNotNotify() {
        val notifications = assignmentNotifications(
            baselineReady = false,
            previous = emptyMap(),
            current = listOf(assignment(status = "pending")),
        )

        assertTrue(notifications.isEmpty())
    }

    @Test
    fun newAssignmentNotifiesAfterBaselineExists() {
        val notifications = assignmentNotifications(
            baselineReady = true,
            previous = emptyMap(),
            current = listOf(assignment(status = "pending")),
        )

        assertEquals(1, notifications.size)
        assertEquals("New referee appointment", notifications.single().title)
        assertEquals("Home FC vs Away FC", notifications.single().body)
    }

    @Test
    fun statusChangeNotifies() {
        val previous = assignment(status = "pending")
        val notifications = assignmentNotifications(
            baselineReady = true,
            previous = mapOf(previous.assignmentKey to previous),
            current = listOf(assignment(status = "accepted")),
        )

        assertEquals(1, notifications.size)
        assertEquals("Referee appointment updated", notifications.single().title)
        assertEquals("Home FC vs Away FC: Accepted", notifications.single().body)
    }

    @Test
    fun unchangedAssignmentDoesNotNotify() {
        val previous = assignment(status = "accepted")
        val notifications = assignmentNotifications(
            baselineReady = true,
            previous = mapOf(previous.assignmentKey to previous),
            current = listOf(previous),
        )

        assertTrue(notifications.isEmpty())
    }

    private fun assignment(
        status: String?,
    ) = RefereeAssignmentSnapshotEntity(
        ownerAccountId = "owner",
        assignmentKey = "allocation-1",
        profileId = "profile",
        fixtureId = "fixture",
        allocationId = "allocation-1",
        refereeStatus = status,
        homeTeamName = "Home FC",
        awayTeamName = "Away FC",
    )
}
