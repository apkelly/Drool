@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.github.apkelly.drool.schedule.background

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.apkelly.drool.common.di.initDroolKoin
import com.github.apkelly.drool.common.domain.model.AssignmentPollingInterval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import platform.Foundation.NSLog
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

interface IosAssignmentPollingSchedulerHandler {
    fun schedule(
        intervalSeconds: Double,
    )
}

object IosAssignmentPollingSchedulerRegistry {
    private var handler: IosAssignmentPollingSchedulerHandler? = null

    fun register(
        handler: IosAssignmentPollingSchedulerHandler,
    ) {
        this.handler = handler
    }

    fun schedule(
        intervalSeconds: Double,
    ) {
        handler?.schedule(intervalSeconds)
    }
}

object IosAssignmentPollBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun execute(
        completion: (Boolean) -> Unit,
    ) {
        initDroolKoin()
        job?.cancel()
        job = scope.launch {
            completion(KoinPlatform.getKoin().get<AssignmentPollExecutor>().execute())
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

@Suppress("UNUSED_PARAMETER")
actual fun createPlatformAssignmentPollingScheduler(
    executor: AssignmentPollExecutor,
): AssignmentPollingScheduler = object : AssignmentPollingScheduler {
    override fun schedule(
        interval: AssignmentPollingInterval,
    ) {
        val seconds = interval.intervalMillis?.div(MILLIS_PER_SECOND)?.toDouble() ?: 0.0
        IosAssignmentPollingSchedulerRegistry.schedule(seconds)
    }
}

actual fun createPlatformRefereeAssignmentNotifier(): RefereeAssignmentNotifier = IosRefereeAssignmentNotifier()

@Composable
actual fun rememberAssignmentNotificationPermissionRequester(): (() -> Unit)? {
    val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
    val scope = rememberCoroutineScope()
    var shouldRequest by remember { mutableStateOf(false) }
    LaunchedEffect(notificationCenter) {
        notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
            scope.launch {
                shouldRequest =
                    settings?.authorizationStatus == UNAuthorizationStatusNotDetermined
            }
        }
    }
    val request = remember(notificationCenter, scope) {
        {
            notificationCenter
                .requestAuthorizationWithOptions(
                    options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
                    completionHandler = { _, _ ->
                        scope.launch { shouldRequest = false }
                    },
                )
        }
    }
    return request.takeIf { shouldRequest }
}

private class IosRefereeAssignmentNotifier : RefereeAssignmentNotifier {
    override fun notify(
        notifications: List<RefereeAssignmentNotification>,
    ) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        notifications.forEach { notification ->
            val content = UNMutableNotificationContent().apply {
                setTitle(notification.title)
                setBody(notification.body)
                setSound(UNNotificationSound.defaultSound)
            }
            center.addNotificationRequest(
                request = UNNotificationRequest.requestWithIdentifier(
                    identifier = notification.id,
                    content = content,
                    trigger = null,
                ),
                withCompletionHandler = { error ->
                    if (error != null) {
                        NSLog(
                            "Unable to display referee assignment notification: %@",
                            error.localizedDescription,
                        )
                    }
                },
            )
        }
    }
}

private const val MILLIS_PER_SECOND = 1_000L
