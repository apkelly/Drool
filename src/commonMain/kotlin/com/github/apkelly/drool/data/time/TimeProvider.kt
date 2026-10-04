package com.github.apkelly.drool.data.time

fun interface TimeProvider {
    fun nowEpochMillis(): Long
}

expect fun platformEpochMillis(): Long

object SystemTimeProvider : TimeProvider {
    override fun nowEpochMillis(): Long = platformEpochMillis()
}
