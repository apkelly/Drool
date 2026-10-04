package com.github.apkelly.drool.data.time

import kotlin.time.Clock

actual fun platformEpochMillis(): Long =
    Clock.System.now().toEpochMilliseconds()
