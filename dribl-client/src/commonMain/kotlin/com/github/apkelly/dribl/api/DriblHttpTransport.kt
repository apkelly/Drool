package com.github.apkelly.dribl.api

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

internal class DriblHttpTransport(
    @PublishedApi internal val client: HttpClient,
    @PublishedApi internal val baseUrl: String,
) {
    internal suspend inline fun <reified T> getBody(
        path: String,
        bearerToken: String,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val url = url(path)
        return client.get(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            configure()
        }.requireBody(url)
    }

    internal fun url(
        path: String,
    ): String = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
