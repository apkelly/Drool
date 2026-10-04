package com.github.apkelly.drool.domain.model

data class CachedData<T>(
    val value: T,
    val lastUpdatedEpochMillis: Long?,
    val isStale: Boolean,
)

sealed interface RefreshResult {
    data object Updated : RefreshResult
    data object NotModified : RefreshResult
    data class Failed(
        val reason: RefreshFailure,
        val hasCachedData: Boolean,
    ) : RefreshResult
}

enum class RefreshFailure {
    Offline,
    Unauthorized,
    Forbidden,
    Server,
    InvalidResponse,
    Unknown,
}
