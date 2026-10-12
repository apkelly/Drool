package com.github.apkelly.drool.schedule.background

import androidx.compose.runtime.Composable
import com.github.apkelly.drool.common.domain.model.AssignmentPollingInterval
import com.github.apkelly.drool.common.logging.DroolLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.AWTException
import java.awt.Color
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

actual fun createPlatformAssignmentPollingScheduler(
    executor: AssignmentPollExecutor,
): AssignmentPollingScheduler = DesktopAssignmentPollingScheduler(executor)

actual fun createPlatformRefereeAssignmentNotifier(): RefereeAssignmentNotifier = DesktopRefereeAssignmentNotifier()

@Composable
actual fun rememberAssignmentNotificationPermissionRequester(): (() -> Unit)? = null

private class DesktopAssignmentPollingScheduler(
    private val executor: AssignmentPollExecutor,
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : AssignmentPollingScheduler {
    private var pollingJob: Job? = null

    override fun schedule(
        interval: AssignmentPollingInterval,
    ) {
        pollingJob?.cancel()
        pollingJob = interval.intervalMillis?.let { intervalMillis ->
            scope.launch {
                while (isActive) {
                    val successful = executor.execute()
                    delay(if (successful) intervalMillis else RETRY_DELAY_MILLIS)
                }
            }
        }
    }
}

private class DesktopRefereeAssignmentNotifier : RefereeAssignmentNotifier {
    private val logger = DroolLog.withTag("AssignmentNotifier")
    private val trayIcon: TrayIcon? by lazy(::createTrayIcon)

    override fun notify(
        notifications: List<RefereeAssignmentNotification>,
    ) {
        notifications.forEach { notification ->
            trayIcon?.displayMessage(
                notification.title,
                notification.body,
                TrayIcon.MessageType.INFO,
            )
        }
    }

    private fun createTrayIcon(): TrayIcon? {
        if (!SystemTray.isSupported()) return null
        val image = BufferedImage(TRAY_ICON_SIZE, TRAY_ICON_SIZE, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().use { graphics ->
            graphics.color = Color(TRAY_ICON_RED, TRAY_ICON_GREEN, TRAY_ICON_BLUE)
            graphics.fillOval(0, 0, TRAY_ICON_SIZE, TRAY_ICON_SIZE)
        }
        val icon = TrayIcon(image, "Drool referee assignments").also {
            it.isImageAutoSize = true
        }
        return try {
            SystemTray.getSystemTray().add(icon)
            icon
        } catch (_: AWTException) {
            logger.w { "Unable to add the referee assignment notification tray icon" }
            null
        }
    }
}

private inline fun <T : java.awt.Graphics> T.use(
    block: (T) -> Unit,
) {
    try {
        block(this)
    } finally {
        dispose()
    }
}

private const val TRAY_ICON_SIZE = 16
private const val TRAY_ICON_RED = 8
private const val TRAY_ICON_GREEN = 122
private const val TRAY_ICON_BLUE = 70
private const val RETRY_DELAY_MILLIS = 15L * 60L * 1_000L
