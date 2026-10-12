package com.github.apkelly.drool.common.domain.operation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CancellationSafeResultTest {
    @Test
    fun cancellationIsRethrown() = runTest {
        assertFailsWith<CancellationException> {
            cancellationSafeResult<Unit> { throw CancellationException("cancelled") }
        }
    }

    @Test
    fun nonCancellationFailuresAreValues() = runTest {
        assertIs<IllegalStateException>(
            cancellationSafeResult<Unit> { error("failed") }.exceptionOrNull(),
        )
    }
}
