package com.github.apkelly.drool.common.data.time

import kotlin.time.Clock

actual fun platformEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
