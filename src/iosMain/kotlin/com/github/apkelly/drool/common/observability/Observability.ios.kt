package com.github.apkelly.drool.common.observability

import kotlin.concurrent.Volatile

interface IosObservabilityHandler {
    fun setCollectionEnabled(
        enabled: Boolean,
    )
    fun logEvent(
        name: String,
        parameterName: String?,
        parameterValue: String?,
    )
    fun recordNonFatal(
        errorType: String,
        operation: String,
    )
}

object IosObservabilityRegistry {
    private var handler: IosObservabilityHandler? = null

    fun register(
        handler: IosObservabilityHandler,
    ) {
        this.handler = handler
    }

    internal fun current(): IosObservabilityHandler? = handler
}

private object IosObservability : Observability {
    @Volatile
    private var collectionEnabled = false

    override fun setCollectionEnabled(
        enabled: Boolean,
    ) {
        collectionEnabled = enabled
        IosObservabilityRegistry.current()?.setCollectionEnabled(enabled)
    }

    override fun log(
        event: AnalyticsEvent,
    ) {
        if (!collectionEnabled) return
        val payload = event.toPayload()
        IosObservabilityRegistry.current()?.logEvent(
            name = payload.name,
            parameterName = payload.parameterName,
            parameterValue = payload.parameterValue,
        )
    }

    override fun recordNonFatal(
        error: Throwable,
        operation: NonFatalOperation,
    ) {
        if (!collectionEnabled) return
        IosObservabilityRegistry.current()?.recordNonFatal(
            errorType = error::class.simpleName ?: "Exception",
            operation = operation.eventValue,
        )
    }
}

actual fun createPlatformObservability(): Observability = IosObservability

actual val platformSupportsObservability: Boolean = true
