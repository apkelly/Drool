package com.github.apkelly.drool.ui.model

import com.github.apkelly.drool.domain.model.RefreshFailure

data class CollectionUiState<T>(
    val items: List<T> = emptyList(),
    val lastUpdatedEpochMillis: Long? = null,
    val isStale: Boolean = true,
    val isRefreshing: Boolean = false,
    val refreshFailure: RefreshFailure? = null,
)
