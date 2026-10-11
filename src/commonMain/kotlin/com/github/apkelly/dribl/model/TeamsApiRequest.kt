package com.github.apkelly.dribl.model

import io.ktor.http.encodeURLParameter

data class TeamsApiRequest(
    val baseUrl: String,
    val path: String,
    val method: String = "GET",
    val bearerToken: String?,
    val queryParameters: Map<String, String>,
) {
    val url: String = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
    val urlWithQuery: String = if (queryParameters.isEmpty()) {
        url
    } else {
        queryParameters.entries.joinToString(prefix = "$url?", separator = "&") { (key, value) ->
            "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
        }
    }
}
