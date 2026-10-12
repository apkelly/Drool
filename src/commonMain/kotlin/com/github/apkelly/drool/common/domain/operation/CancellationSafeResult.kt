package com.github.apkelly.drool.common.domain.operation

import kotlinx.coroutines.CancellationException

@Suppress("TooGenericExceptionCaught")
suspend inline fun <T> cancellationSafeResult(
    block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    Result.failure(error)
}
