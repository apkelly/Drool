package com.github.apkelly.drool.common.data.repository

import com.github.apkelly.dribl.api.DriblHttpException
import com.github.apkelly.dribl.api.DriblResponseException
import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.local.entity.CacheMetadataEntity
import com.github.apkelly.drool.common.data.remote.SportsRemoteDataSource
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.domain.model.OperationResult
import com.github.apkelly.drool.common.domain.model.RefreshFailure
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import com.github.apkelly.drool.common.domain.operation.cancellationSafeResult
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

internal suspend fun profileToken(
    api: SportsRemoteDataSource,
    store: BearerTokenStore,
    profileId: String,
): String {
    val rootToken = store.getBearerToken()
        ?: throw DriblResponseException("No authenticated session")
    val rootProfileId = store.getActiveAccountId()
        ?: throw DriblResponseException("No active profile")
    return if (profileId == rootProfileId) {
        rootToken
    } else {
        api.createProfileSession(rootToken, profileId)
    }
}

internal suspend fun refreshResult(
    hasCachedData: Boolean,
    block: suspend () -> Unit,
): RefreshResult = cancellationSafeResult { block() }.fold(
    onSuccess = { RefreshResult.Updated },
    onFailure = { RefreshResult.Failed(it.toRefreshFailure(), hasCachedData) },
)

internal suspend fun <T> operationResult(
    block: suspend () -> T,
): OperationResult<T> = cancellationSafeResult { block() }.fold(
    onSuccess = OperationResult<T>::Success,
    onFailure = { OperationResult.Failed(it.toRefreshFailure()) },
)

internal suspend fun DroolDatabase.markUpdated(
    key: String,
    accountScope: String,
    nowEpochMillis: Long,
) {
    cacheMetadataDao().upsert(
        CacheMetadataEntity(key, accountScope, nowEpochMillis),
    )
}

internal fun CacheMetadataEntity?.isStale(
    nowEpochMillis: Long,
    ttlMillis: Long,
): Boolean = this == null || nowEpochMillis - updatedAtEpochMillis >= ttlMillis

internal fun relationshipFrom(
    value: String,
): TeamRelationship = TeamRelationship.entries.firstOrNull { it.name == value }
    ?: TeamRelationship.None

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403

private fun Throwable.toRefreshFailure(): RefreshFailure = when (this) {
    is DriblHttpException -> when (statusCode) {
        HTTP_UNAUTHORIZED -> RefreshFailure.Unauthorized
        HTTP_FORBIDDEN -> RefreshFailure.Forbidden
        in 500..599 -> RefreshFailure.Server
        else -> RefreshFailure.InvalidResponse
    }

    is DriblResponseException,
    is SerializationException,
    -> RefreshFailure.InvalidResponse

    is IOException -> RefreshFailure.Offline

    else -> RefreshFailure.Unknown
}
