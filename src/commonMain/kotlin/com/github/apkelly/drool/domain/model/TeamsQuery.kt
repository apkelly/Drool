package com.github.apkelly.drool.domain.model

data class TeamsQuery(
    val baseUrl: String,
    val path: String,
    val method: String,
    val bearerToken: String?,
    val queryParameters: Map<String, String>,
) {
    val url: String = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
