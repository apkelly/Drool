package com.github.apkelly.drool.schedule.background

import androidx.compose.runtime.Composable
import com.github.apkelly.drool.common.domain.model.AssignmentPollingInterval

fun interface AssignmentPollingScheduler {
    fun schedule(
        interval: AssignmentPollingInterval,
    )
}

fun interface RefereeAssignmentNotifier {
    fun notify(
        notifications: List<RefereeAssignmentNotification>,
    )
}

class AssignmentPollExecutor(
    private val poller: RefereeAssignmentPoller,
    private val notifier: RefereeAssignmentNotifier,
) {
    suspend fun execute(): Boolean = when (val result = poller.poll()) {
        AssignmentPollResult.NoSession -> true

        AssignmentPollResult.Retry -> false

        is AssignmentPollResult.Completed -> {
            notifier.notify(result.notifications)
            true
        }
    }
}

expect fun createPlatformAssignmentPollingScheduler(
    executor: AssignmentPollExecutor,
): AssignmentPollingScheduler

expect fun createPlatformRefereeAssignmentNotifier(): RefereeAssignmentNotifier

@Composable
expect fun rememberAssignmentNotificationPermissionRequester(): (() -> Unit)?
