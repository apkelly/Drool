package com.github.apkelly.dribl.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TOKEN_REFRESH_URL = "https://api.dribl.com/api/auth/token/refresh"

interface DriblTokenStore {
    suspend fun getBearerToken(): String?
    suspend fun saveBearerToken(
        token: String,
    )
}

class DriblTokenRenewer(
    private val renewalClient: HttpClient,
    private val store: DriblTokenStore,
) {
    private val mutex = Mutex()
    private val replacements = mutableMapOf<String, String>()

    suspend fun currentToken(
        token: String,
    ): String = mutex.withLock {
        resolveReplacement(token)
    }

    suspend fun renew(
        token: String,
    ): String? = mutex.withLock {
        val current = resolveReplacement(token)
        if (current != token) return current

        val response = renewalClient.post(TOKEN_REFRESH_URL) {
            accept(ContentType.Text.Plain)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(token)
        }
        if (!response.status.isSuccess()) return null

        val renewed = response.bodyAsText().trim().trim('"')
            .takeIf(::isJwt) ?: return null
        replacements[token] = renewed
        if (store.getBearerToken() == token) {
            store.saveBearerToken(renewed)
        }
        renewed
    }

    private fun resolveReplacement(
        token: String,
    ): String {
        var current = token
        val visited = mutableListOf<String>()
        while (true) {
            val replacement = replacements[current] ?: break
            visited += current
            current = replacement
        }
        visited.forEach { replacements[it] = current }
        return current
    }
}

fun HttpClient.withAutomaticDriblTokenRenewal(
    renewer: DriblTokenRenewer,
): HttpClient = apply {
    plugin(HttpSend).intercept { request ->
        val requestedToken = request.headers[HttpHeaders.Authorization]
            ?.substringAfter("Bearer ", missingDelimiterValue = "")
            ?.takeIf(String::isNotBlank)
        if (requestedToken == null) {
            return@intercept execute(request)
        }

        val currentToken = renewer.currentToken(requestedToken)
        if (currentToken != requestedToken) {
            request.headers.remove(HttpHeaders.Authorization)
            request.bearerAuth(currentToken)
        }
        val call = execute(request)
        if (call.response.status != HttpStatusCode.Unauthorized) {
            return@intercept call
        }

        val renewedToken = renewer.renew(currentToken) ?: return@intercept call
        call.response.bodyAsText()
        request.headers.remove(HttpHeaders.Authorization)
        request.bearerAuth(renewedToken)
        execute(request)
    }
}

private fun isJwt(
    value: String,
): Boolean = value.length >= MINIMUM_JWT_LENGTH &&
    value.split('.').size == JWT_SEGMENT_COUNT &&
    value.none(Char::isWhitespace)

private const val MINIMUM_JWT_LENGTH = 32
private const val JWT_SEGMENT_COUNT = 3
