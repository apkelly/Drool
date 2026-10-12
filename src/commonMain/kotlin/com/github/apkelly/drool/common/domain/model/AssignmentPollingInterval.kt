package com.github.apkelly.drool.common.domain.model

enum class AssignmentPollingInterval(
    val intervalMillis: Long?,
) {
    Off(null),
    Hourly(60L * 60L * 1_000L),
    EveryThreeHours(3L * 60L * 60L * 1_000L),
    EverySixHours(6L * 60L * 60L * 1_000L),
}
