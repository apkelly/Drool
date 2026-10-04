package com.github.apkelly.drool.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtProfileMapperTest {
    @Test
    fun mapsStandardJwtIdentityClaims() {
        val token =
            "header.eyJzdWIiOiJhY2NvdW50IiwibmFtZSI6IkFsZXgiLCJlbWFpbCI6ImFsZXhAZXhhbXBsZS5jb20ifQ.signature"

        val profile = profileFromBearerToken(token)

        assertEquals("account", profile?.accountId)
        assertEquals("Alex", profile?.displayName)
        assertEquals("alex@example.com", profile?.email)
    }

    @Test
    fun mapsNestedClaimsAndFallsBackToEmailForDisplayName() {
        val token =
            "header.eyJ1c2VyIjp7InVzZXJfaWQiOiI0MiIsImVtYWlsIjoicGxheWVyQGV4YW1wbGUuY29tIn19.signature"

        val profile = profileFromBearerToken(token)

        assertEquals("42", profile?.accountId)
        assertEquals("player@example.com", profile?.displayName)
    }

    @Test
    fun mapsUsernameAndComposedNameClaims() {
        val usernameToken =
            "header.eyJzdWIiOiI2NDAzMTE4IiwidXNlcm5hbWUiOiJhbmR5In0.signature"
        val composedNameToken =
            "header.eyJzdWIiOiI2NDAzMTE4IiwiZmlyc3RfbmFtZSI6IkFuZHkiLCJsYXN0X25hbWUiOiJQbGF5ZXIifQ.signature"

        assertEquals("andy", profileFromBearerToken(usernameToken)?.displayName)
        assertEquals("Andy", profileFromBearerToken(composedNameToken)?.displayName)
    }

    @Test
    fun firstAndLastNameOverrideAnEmailValuedNameClaim() {
        val token =
            "header.eyJzdWIiOiJhY2NvdW50IiwibmFtZSI6ImFsZXhAZXhhbXBsZS5jb20iLCJmaXJzdF9uYW1lIjoiQWxleCIsImxhc3RfbmFtZSI6IlBsYXllciIsImVtYWlsIjoiYWxleEBleGFtcGxlLmNvbSJ9.signature"

        assertEquals("Alex", profileFromBearerToken(token)?.displayName)
    }

    @Test
    fun rejectsOpaqueMalformedAndIdentityFreeTokens() {
        assertNull(profileFromBearerToken("opaque-token"))
        assertNull(profileFromBearerToken("header.!.signature"))
        assertNull(profileFromBearerToken("header.e30.signature"))
    }
}
