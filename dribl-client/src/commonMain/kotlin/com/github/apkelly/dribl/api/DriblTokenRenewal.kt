package com.github.apkelly.dribl.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.bearerAuth
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

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
