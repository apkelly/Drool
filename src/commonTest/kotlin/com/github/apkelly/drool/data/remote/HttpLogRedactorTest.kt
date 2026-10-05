package com.github.apkelly.drool.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

class HttpLogRedactorTest {
    @Test
    fun redactsCredentialsAndTokens() {
        val message =
            """{"username":"alex","password":"secret","access_token":"jwt","user":{"id":42}}"""

        assertEquals(
            """{"username":"alex","password":"██","access_token":"██","user":{"id":42}}""",
            redactSensitiveHttpLogMessage(message),
        )
    }

    @Test
    fun redactsCaseVariantsAndEscapedValues() {
        val message =
            """{"authToken":"abc\"def","client_secret":"secret","displayName":"Alex"}"""

        assertEquals(
            """{"authToken":"██","client_secret":"██","displayName":"Alex"}""",
            redactSensitiveHttpLogMessage(message),
        )
    }
}
