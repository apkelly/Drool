package com.github.apkelly.drool.common.ui.viewmodel

import com.github.apkelly.drool.common.domain.model.CachedData
import com.github.apkelly.drool.common.domain.model.RefreshFailure
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.observability.AnalyticsEvent
import com.github.apkelly.drool.common.observability.Observability
import com.github.apkelly.drool.common.observability.RefreshArea
import com.github.apkelly.drool.common.observability.RefreshOutcome
import com.github.apkelly.drool.common.ui.model.CollectionUiState

internal fun <T> collectionState(
    data: CachedData<List<T>>,
    refreshing: Boolean,
    failure: RefreshFailure?,
) = CollectionUiState(
    items = data.value,
    lastUpdatedEpochMillis = data.lastUpdatedEpochMillis,
    isStale = data.isStale,
    isRefreshing = refreshing,
    refreshFailure = failure,
)

internal fun Observability.trackRefresh(
    area: RefreshArea,
    result: RefreshResult,
) {
    log(
        AnalyticsEvent.RefreshCompleted(
            area = area,
            outcome = when (result) {
                RefreshResult.Updated -> RefreshOutcome.Updated
                RefreshResult.NotModified -> RefreshOutcome.NotModified
                is RefreshResult.Failed -> RefreshOutcome.Failed
            },
        ),
    )
}
