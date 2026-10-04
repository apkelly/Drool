package com.github.apkelly.drool.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

class HttpLogRedactorTest {
    @Test
    fun leavesCredentialsAndTokensVisibleForDebugging() {
        val message =
            """{"username":"alex","password":"secret","access_token":"jwt","user":{"id":42}}"""

        assertEquals(message, redactSensitiveHttpLogMessage(message))
    }

    @Test
    fun leavesCaseVariantsAndEscapedValuesVisibleForDebugging() {
        val message =
            """{"authToken":"abc\"def","client_secret":"secret","displayName":"Alex"}"""

        assertEquals(message, redactSensitiveHttpLogMessage(message))
    }
}
