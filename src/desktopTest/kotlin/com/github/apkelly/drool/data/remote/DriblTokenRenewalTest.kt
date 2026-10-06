package com.github.apkelly.drool.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.nio.file.Files
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import com.github.apkelly.drool.data.storage.createBearerTokenStore
import kotlin.test.Test
import kotlin.test.assertEquals

class DriblTokenRenewalTest {
    @Test
    fun renewsPersistsAndRetriesUnauthorizedRequest() = runTest {
        val oldToken = jwt("old")
        val newToken = jwt("new")
        val tokenFile = Files.createTempFile("drool-token-renewal", ".preferences_pb")
            .toFile()
            .apply { delete() }
        val store = createBearerTokenStore { tokenFile.absolutePath }
        store.saveBearerToken(oldToken)

        var renewalRequests = 0
        val renewalClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    renewalRequests += 1
                    assertEquals(
                        "Bearer $oldToken",
                        request.headers[HttpHeaders.Authorization],
                    )
                    respond(newToken, HttpStatusCode.OK)
                }
            }
        }
        val receivedTokens = mutableListOf<String?>()
        val sportsClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    receivedTokens += request.headers[HttpHeaders.Authorization]
                    if (receivedTokens.size == 1) {
                        respond("expired", HttpStatusCode.Unauthorized)
                    } else {
                        respond(
                            "success",
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "text/plain"),
                        )
                    }
                }
            }
        }.withAutomaticDriblTokenRenewal(
            DriblTokenRenewer(renewalClient, store)
        )

        val response = sportsClient.get("https://api.dribl.com/api/test") {
            bearerAuth(oldToken)
        }

        assertEquals("success", response.bodyAsText())
        assertEquals<List<String?>>(
            listOf("Bearer $oldToken", "Bearer $newToken"),
            receivedTokens,
        )
        assertEquals(1, renewalRequests)
        assertEquals(newToken, store.getBearerToken())

        sportsClient.close()
        renewalClient.close()
        tokenFile.delete()
    }

    @Test
    fun returnsOriginalUnauthorizedResponseWhenRenewalFails() = runTest {
        val oldToken = jwt("old")
        val tokenFile = Files.createTempFile("drool-token-renewal", ".preferences_pb")
            .toFile()
            .apply { delete() }
        val store = createBearerTokenStore { tokenFile.absolutePath }
        store.saveBearerToken(oldToken)

        val renewalClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond("rejected", HttpStatusCode.Unauthorized)
                }
            }
        }
        var sportsRequests = 0
        val sportsClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    sportsRequests += 1
                    respond("expired", HttpStatusCode.Unauthorized)
                }
            }
        }.withAutomaticDriblTokenRenewal(
            DriblTokenRenewer(renewalClient, store)
        )

        val response = sportsClient.get("https://api.dribl.com/api/test") {
            bearerAuth(oldToken)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(1, sportsRequests)
        assertEquals(oldToken, store.getBearerToken())

        sportsClient.close()
        renewalClient.close()
        tokenFile.delete()
    }

    @Test
    fun reusesRenewedTokenWithoutAnotherUnauthorizedRequest() = runTest {
        val oldToken = jwt("old")
        val newToken = jwt("new")
        val tokenFile = Files.createTempFile("drool-token-renewal", ".preferences_pb")
            .toFile()
            .apply { delete() }
        val store = createBearerTokenStore { tokenFile.absolutePath }
        store.saveBearerToken(oldToken)

        var renewalRequests = 0
        val renewalClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    renewalRequests += 1
                    respond(newToken, HttpStatusCode.OK)
                }
            }
        }
        val receivedTokens = mutableListOf<String?>()
        val sportsClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val authorization = request.headers[HttpHeaders.Authorization]
                    receivedTokens += authorization
                    respond(
                        if (authorization == "Bearer $oldToken") "expired" else "success",
                        if (authorization == "Bearer $oldToken") {
                            HttpStatusCode.Unauthorized
                        } else {
                            HttpStatusCode.OK
                        },
                    )
                }
            }
        }.withAutomaticDriblTokenRenewal(
            DriblTokenRenewer(renewalClient, store)
        )

        repeat(2) {
            assertEquals(
                HttpStatusCode.OK,
                sportsClient.get("https://api.dribl.com/api/test") {
                    bearerAuth(oldToken)
                }.status,
            )
        }

        assertEquals<List<String?>>(
            listOf(
                "Bearer $oldToken",
                "Bearer $newToken",
                "Bearer $newToken",
            ),
            receivedTokens,
        )
        assertEquals(1, renewalRequests)

        sportsClient.close()
        renewalClient.close()
        tokenFile.delete()
    }

    @Test
    fun concurrentUnauthorizedRequestsShareOneRenewal() = runTest {
        val oldToken = jwt("old")
        val newToken = jwt("new")
        val tokenFile = Files.createTempFile("drool-token-renewal", ".preferences_pb")
            .toFile()
            .apply { delete() }
        val store = createBearerTokenStore { tokenFile.absolutePath }
        store.saveBearerToken(oldToken)

        var renewalRequests = 0
        val renewalClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    renewalRequests += 1
                    delay(50)
                    respond(newToken, HttpStatusCode.OK)
                }
            }
        }
        val sportsClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.headers[HttpHeaders.Authorization] == "Bearer $oldToken") {
                        respond("expired", HttpStatusCode.Unauthorized)
                    } else {
                        respond("success", HttpStatusCode.OK)
                    }
                }
            }
        }.withAutomaticDriblTokenRenewal(
            DriblTokenRenewer(renewalClient, store)
        )

        val statuses = List(2) {
            async {
                sportsClient.get("https://api.dribl.com/api/test") {
                    bearerAuth(oldToken)
                }.status
            }
        }.awaitAll()

        assertEquals(
            listOf(HttpStatusCode.OK, HttpStatusCode.OK),
            statuses,
        )
        assertEquals(1, renewalRequests)
        assertEquals(newToken, store.getBearerToken())

        sportsClient.close()
        renewalClient.close()
        tokenFile.delete()
    }
}

private fun jwt(marker: String): String =
    "header.${marker.padEnd(32, 'x')}.signature"
