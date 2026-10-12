package com.github.apkelly.dribl.api

class DriblHttpException(
    val statusCode: Int,
    url: String,
    responseBody: String,
) : IllegalStateException(
    "HTTP $statusCode for $url\n${responseBody.take(MAX_ERROR_BODY_LENGTH)}",
)

private const val MAX_ERROR_BODY_LENGTH = 2_000
