package com.github.apkelly.drool.common.observability

import android.os.Bundle
import com.github.apkelly.drool.common.data.storage.requireAndroidApplicationContext
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

private class FirebaseObservability(
    private val analyticsProvider: () -> FirebaseAnalytics = {
        FirebaseAnalytics.getInstance(requireAndroidApplicationContext())
    },
    private val crashlyticsProvider: () -> FirebaseCrashlytics = {
        FirebaseCrashlytics.getInstance()
    },
) : Observability {
    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null
    private var collectionEnabled = false

    override fun setCollectionEnabled(
        enabled: Boolean,
    ) {
        collectionEnabled = enabled
        if (enabled) {
            analytics().setAnalyticsCollectionEnabled(true)
            crashlytics().setCrashlyticsCollectionEnabled(true)
        } else {
            analytics?.setAnalyticsCollectionEnabled(false)
            crashlytics?.setCrashlyticsCollectionEnabled(false)
        }
    }

    override fun log(
        event: AnalyticsEvent,
    ) {
        if (!collectionEnabled) return
        val payload = event.toPayload()
        val parameters = payload.parameterName?.let { name ->
            Bundle().apply { putString(name, payload.parameterValue) }
        }
        analytics().logEvent(payload.name, parameters)
    }

    override fun recordNonFatal(
        error: Throwable,
        operation: NonFatalOperation,
    ) {
        if (!collectionEnabled) return
        val sanitized = RuntimeException(
            "${error::class.simpleName ?: "Exception"} during ${operation.eventValue}",
        ).apply {
            stackTrace = error.stackTrace
        }
        crashlytics().setCustomKey("operation", operation.eventValue)
        crashlytics().recordException(sanitized)
    }

    private fun analytics(): FirebaseAnalytics = analytics ?: analyticsProvider().also { analytics = it }

    private fun crashlytics(): FirebaseCrashlytics = crashlytics ?: crashlyticsProvider().also { crashlytics = it }
}

actual fun createPlatformObservability(): Observability = FirebaseObservability()

actual val platformSupportsObservability: Boolean = true
