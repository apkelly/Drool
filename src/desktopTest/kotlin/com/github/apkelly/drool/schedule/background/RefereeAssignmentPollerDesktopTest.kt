package com.github.apkelly.drool.schedule.background

import com.github.apkelly.drool.app.FakeSessionDataSource
import com.github.apkelly.drool.app.FakeSportsDataSource
import com.github.apkelly.drool.common.data.local.entity.FixtureEntity
import com.github.apkelly.drool.common.data.repository.TestDatabase
import com.github.apkelly.drool.common.data.repository.TestTokenStore
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.RefreshFailure
import com.github.apkelly.drool.common.domain.model.RefreshResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RefereeAssignmentPollerDesktopTest {
    @Test
    fun noSessionCompletesWithoutRefreshing() = runTest {
        fixture { poller, _, sports, _ ->
            assertEquals(AssignmentPollResult.NoSession, poller.poll())
            assertEquals(null, sports.lastLinkedProfiles)
        }
    }

    @Test
    fun everyProfileRefreshFailureRequestsRetry() = runTest {
        fixture { poller, session, sports, _ ->
            session.profile.value = Profile("owner", "Owner", null)
            sports.linkedProfileRefreshResults = mapOf(
                "owner" to RefreshResult.Failed(
                    reason = RefreshFailure.Offline,
                    hasCachedData = false,
                ),
            )

            assertEquals(AssignmentPollResult.Retry, poller.poll())
            assertEquals(true, sports.lastForce)
        }
    }

    @Test
    fun initialPollStoresBaselineAndLaterAssignmentNotifies() = runTest {
        fixture { poller, session, _, database ->
            session.profile.value = Profile("owner", "Owner", null)
            database.fixtureDao().upsertAll(listOf(assignment("fixture-1", "allocation-1")))

            val initial = assertIs<AssignmentPollResult.Completed>(poller.poll())
            assertTrue(initial.notifications.isEmpty())
            assertEquals(
                listOf("allocation-1"),
                database.refereeAssignmentSnapshotDao()
                    .getForOwner("owner")
                    .map { it.assignmentKey },
            )

            database.fixtureDao().upsertAll(listOf(assignment("fixture-2", "allocation-2")))
            val updated = assertIs<AssignmentPollResult.Completed>(poller.poll())

            assertEquals(1, updated.notifications.size)
            assertEquals("New referee appointment", updated.notifications.single().title)
        }
    }

    private suspend fun fixture(
        test: suspend (
            RefereeAssignmentPoller,
            FakeSessionDataSource,
            FakeSportsDataSource,
            com.github.apkelly.drool.common.data.local.DroolDatabase,
        ) -> Unit,
    ) {
        TestDatabase().use { database ->
            TestTokenStore().use { tokenStore ->
                val session = FakeSessionDataSource()
                val sports = FakeSportsDataSource()
                test(
                    RefereeAssignmentPoller(
                        sessionDataSource = session,
                        sportsDataSource = sports,
                        database = database.database,
                        store = tokenStore.store,
                    ),
                    session,
                    sports,
                    database.database,
                )
            }
        }
    }

    private fun assignment(
        fixtureId: String,
        allocationId: String,
    ) = FixtureEntity(
        id = fixtureId,
        accountId = "owner",
        kickoffEpochMillis = 1L,
        homeTeamId = "home",
        homeTeamName = "Home FC",
        awayTeamId = "away",
        awayTeamName = "Away FC",
        competitionName = "Premier",
        venueName = "Main Oval",
        userTeamId = null,
        status = "Scheduled",
        role = FixtureRole.Referee.name,
        refereeRole = "Referee",
        refereeAllocationId = allocationId,
        refereeStatus = "pending",
    )
}
