package com.github.apkelly.drool.schedule.background

import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.local.entity.CacheMetadataEntity
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
        val baselineReady = store.getAssignmentBaselineAccountId() == profile.accountId &&
            database.cacheMetadataDao().get(ASSIGNMENT_BASELINE_KEY, profile.accountId) != null
        val failedProfileIds = results.filterValues { it is RefreshResult.Failed }.keys
        if (
            results.values.all { it is RefreshResult.Failed } ||
            (!baselineReady && failedProfileIds.isNotEmpty())
        ) {
            return AssignmentPollResult.Retry
        }

        val succeededProfileIds = profiles.map { it.id }.filterNot { it in failedProfileIds }
        val assignments = database.fixtureDao()
            .getRefereeFixtures(succeededProfileIds)
            .map { it.toAssignmentSnapshot(profile.accountId) }
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
        val preservedFromFailed = previous.values.filter { it.profileId in failedProfileIds }
        database.replaceAssignmentSnapshots(
            ownerAccountId = profile.accountId,
            assignments = assignments + preservedFromFailed,
        )
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

private const val ASSIGNMENT_BASELINE_KEY = "referee_assignment_baseline"

private suspend fun DroolDatabase.replaceAssignmentSnapshots(
    ownerAccountId: String,
    assignments: List<RefereeAssignmentSnapshotEntity>,
) {
    inTransaction {
        refereeAssignmentSnapshotDao().deleteForOwner(ownerAccountId)
        refereeAssignmentSnapshotDao().upsertAll(assignments)
        cacheMetadataDao().upsert(
            CacheMetadataEntity(
                cacheKey = ASSIGNMENT_BASELINE_KEY,
                accountScope = ownerAccountId,
                updatedAtEpochMillis = 0L,
            ),
        )
    }
}
