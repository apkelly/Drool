package com.github.apkelly.drool.common.data.time

fun interface TimeProvider {
    fun nowEpochMillis(): Long
}

expect fun platformEpochMillis(): Long

object SystemTimeProvider : TimeProvider {
    override fun nowEpochMillis(): Long = platformEpochMillis()
}
