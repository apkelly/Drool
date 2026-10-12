package com.github.apkelly.drool.schedule.background

import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.local.entity.FixtureEntity
import com.github.apkelly.drool.common.data.local.entity.RefereeAssignmentSnapshotEntity
import com.github.apkelly.drool.common.data.local.inTransaction
import com.github.apkelly.drool.common.data.repository.SessionDataSource
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.first

class RefereeAssignmentPoller(
    private val sessionDataSource: SessionDataSource,
    private val scheduleRepository: ScheduleRepository,
    private val database: DroolDatabase,
    private val store: BearerTokenStore,
) {
    suspend fun poll(): AssignmentPollResult {
        val profile = sessionDataSource.observeProfile().first() ?: return AssignmentPollResult.NoSession
        val profiles = profile.linkedProfiles
        val results = scheduleRepository.refreshLinkedProfiles(profiles, force = true)
        if (results.values.all { it is RefreshResult.Failed }) {
            return AssignmentPollResult.Retry
        }

        val assignments = database.fixtureDao()
            .getRefereeFixtures(profiles.map { it.id })
            .map { it.toAssignmentSnapshot(profile.accountId) }
        val baselineReady = store.getAssignmentBaselineAccountId() == profile.accountId
        val previous = if (baselineReady) {
            database.refereeAssignmentSnapshotDao().getForOwner(profile.accountId)
                .associateBy(RefereeAssignmentSnapshotEntity::assignmentKey)
        } else {
            emptyMap()
        }
        val notifications = assignmentNotifications(
            baselineReady = baselineReady,
            previous = previous,
            current = assignments,
        )
        database.replaceAssignmentSnapshots(profile.accountId, assignments)
        store.setAssignmentBaselineAccountId(profile.accountId)
        return AssignmentPollResult.Completed(notifications)
    }
}

sealed interface AssignmentPollResult {
    data object NoSession : AssignmentPollResult
    data object Retry : AssignmentPollResult

    data class Completed(
        val notifications: List<RefereeAssignmentNotification>,
    ) : AssignmentPollResult
}

data class RefereeAssignmentNotification(
    val id: String,
    val title: String,
    val body: String,
)

private fun FixtureEntity.toAssignmentSnapshot(
    ownerAccountId: String,
): RefereeAssignmentSnapshotEntity {
    check(role == FixtureRole.Referee.name)
    val key = refereeAllocationId ?: "$accountId:$id"
    return RefereeAssignmentSnapshotEntity(
        ownerAccountId = ownerAccountId,
        assignmentKey = key,
        profileId = accountId,
        fixtureId = id,
        allocationId = refereeAllocationId,
        refereeStatus = refereeStatus.normalizedStatus(),
        homeTeamName = homeTeamName,
        awayTeamName = awayTeamName,
    )
}

internal fun assignmentNotifications(
    baselineReady: Boolean,
    previous: Map<String, RefereeAssignmentSnapshotEntity>,
    current: List<RefereeAssignmentSnapshotEntity>,
): List<RefereeAssignmentNotification> {
    if (!baselineReady) return emptyList()
    return current.mapNotNull { assignment ->
        assignment.notificationComparedWith(previous[assignment.assignmentKey])
    }
}

private fun RefereeAssignmentSnapshotEntity.notificationComparedWith(
    previous: RefereeAssignmentSnapshotEntity?,
): RefereeAssignmentNotification? = when {
    previous == null -> RefereeAssignmentNotification(
        id = "referee-assignment-$assignmentKey",
        title = "New referee appointment",
        body = "$homeTeamName vs $awayTeamName",
    )

    previous.refereeStatus != refereeStatus -> RefereeAssignmentNotification(
        id = "referee-assignment-status-$assignmentKey",
        title = "Referee appointment updated",
        body = "$homeTeamName vs $awayTeamName: ${refereeStatus.displayStatus()}",
    )

    else -> null
}

private fun String?.normalizedStatus(): String? = this
    ?.trim()
    ?.lowercase()
    ?.takeIf(String::isNotEmpty)

private fun String?.displayStatus(): String = this
    ?.replace('_', ' ')
    ?.replaceFirstChar(Char::uppercase)
    ?: "Status changed"

private suspend fun DroolDatabase.replaceAssignmentSnapshots(
    ownerAccountId: String,
    assignments: List<RefereeAssignmentSnapshotEntity>,
) {
    inTransaction {
        refereeAssignmentSnapshotDao().deleteForOwner(ownerAccountId)
        refereeAssignmentSnapshotDao().upsertAll(assignments)
    }
}
