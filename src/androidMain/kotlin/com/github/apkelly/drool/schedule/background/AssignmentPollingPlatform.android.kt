package com.github.apkelly.drool.schedule.background

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.apkelly.drool.common.data.storage.initializeAndroidStorage
import com.github.apkelly.drool.common.data.storage.requireAndroidApplicationContext
import com.github.apkelly.drool.common.di.initDroolKoin
import com.github.apkelly.drool.common.domain.model.AssignmentPollingInterval
import com.github.apkelly.drool.shared.R
import org.koin.mp.KoinPlatform
import java.util.concurrent.TimeUnit

actual fun createPlatformAssignmentPollingScheduler(
    executor: AssignmentPollExecutor,
): AssignmentPollingScheduler = AndroidAssignmentPollingScheduler(
    requireAndroidApplicationContext(),
)

actual fun createPlatformRefereeAssignmentNotifier(): RefereeAssignmentNotifier =
    AndroidRefereeAssignmentNotifier(requireAndroidApplicationContext())

@Composable
actual fun rememberAssignmentNotificationPermissionRequester(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                (context as? Activity)?.requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE,
                )
            }
        }
    }
}

class RefereeAssignmentWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        initializeAndroidStorage(applicationContext)
        initDroolKoin()
        val successful = KoinPlatform.getKoin().get<AssignmentPollExecutor>().execute()
        return if (successful) Result.success() else Result.retry()
    }
}

private class AndroidAssignmentPollingScheduler(
    context: Context,
) : AssignmentPollingScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun schedule(
        interval: AssignmentPollingInterval,
    ) {
        val intervalMillis = interval.intervalMillis
        if (intervalMillis == null) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequest.Builder(
            RefereeAssignmentWorker::class.java,
            intervalMillis,
            TimeUnit.MILLISECONDS,
        )
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic,
        )
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RefereeAssignmentWorker>()
                .setConstraints(constraints)
                .build(),
        )
    }
}

private class AndroidRefereeAssignmentNotifier(
    private val context: Context,
) : RefereeAssignmentNotifier {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun notify(
        notifications: List<RefereeAssignmentNotification>,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel()
        notifications.forEach { notification ->
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                android.app.Notification.Builder(context)
            }
            builder
                .setSmallIcon(R.drawable.ic_referee_assignment_notification)
                .setContentTitle(notification.title)
                .setContentText(notification.body)
                .setAutoCancel(true)
                .setContentIntent(launchIntent())
            notificationManager.notify(notification.id.hashCode(), builder.build())
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Referee assignments",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private fun launchIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

private const val PERIODIC_WORK_NAME = "referee-assignment-poll"
private const val IMMEDIATE_WORK_NAME = "referee-assignment-poll-now"
private const val NOTIFICATION_CHANNEL_ID = "referee-assignments"
private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 4102
