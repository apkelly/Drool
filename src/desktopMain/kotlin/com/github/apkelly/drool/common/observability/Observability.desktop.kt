package com.github.apkelly.drool.common.observability

actual fun createPlatformObservability(): Observability = NoOpObservability

actual val platformSupportsObservability: Boolean = false
