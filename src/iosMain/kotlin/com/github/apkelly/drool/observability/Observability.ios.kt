package com.github.apkelly.drool.observability

interface IosObservabilityHandler {
    fun setCollectionEnabled(enabled: Boolean)
    fun logEvent(name: String, parameterName: String?, parameterValue: String?)
    fun recordNonFatal(errorType: String, operation: String)
}

object IosObservabilityRegistry {
    private var handler: IosObservabilityHandler? = null

    fun register(handler: IosObservabilityHandler) {
        this.handler = handler
    }

    internal fun current(): IosObservabilityHandler? = handler
}

private object IosObservability : Observability {
    override fun setCollectionEnabled(enabled: Boolean) {
        IosObservabilityRegistry.current()?.setCollectionEnabled(enabled)
    }

    override fun log(event: AnalyticsEvent) {
        val payload = event.toPayload()
        IosObservabilityRegistry.current()?.logEvent(
            name = payload.name,
            parameterName = payload.parameterName,
            parameterValue = payload.parameterValue,
        )
    }

    override fun recordNonFatal(error: Throwable, operation: NonFatalOperation) {
        IosObservabilityRegistry.current()?.recordNonFatal(
            errorType = error::class.simpleName ?: "Exception",
            operation = operation.eventValue,
        )
    }
}

actual fun createPlatformObservability(): Observability = IosObservability
