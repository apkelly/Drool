package com.github.apkelly.drool.data.remote.model

import io.ktor.http.encodeURLParameter
import com.github.apkelly.drool.domain.model.TeamsQuery

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

fun TeamsQuery.toApiRequest(): TeamsApiRequest =
    TeamsApiRequest(
        baseUrl = baseUrl,
        path = path,
        method = method,
        bearerToken = bearerToken,
        queryParameters = queryParameters,
    )
