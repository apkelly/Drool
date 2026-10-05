package com.github.apkelly.drool.observability

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.github.apkelly.drool.data.storage.requireAndroidApplicationContext

private class FirebaseObservability(
    private val analytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(
        requireAndroidApplicationContext()
    ),
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
) : Observability {
    private var collectionEnabled = false

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
        analytics.setAnalyticsCollectionEnabled(enabled)
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }

    override fun log(event: AnalyticsEvent) {
        if (!collectionEnabled) return
        val payload = event.toPayload()
        val parameters = payload.parameterName?.let { name ->
            Bundle().apply { putString(name, payload.parameterValue) }
        }
        analytics.logEvent(payload.name, parameters)
    }

    override fun recordNonFatal(error: Throwable, operation: NonFatalOperation) {
        if (!collectionEnabled) return
        val sanitized = RuntimeException(
            "${error::class.simpleName ?: "Exception"} during ${operation.eventValue}"
        ).apply {
            stackTrace = error.stackTrace
        }
        crashlytics.setCustomKey("operation", operation.eventValue)
        crashlytics.recordException(sanitized)
    }
}

actual fun createPlatformObservability(): Observability = FirebaseObservability()
