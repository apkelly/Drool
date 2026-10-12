package com.github.apkelly.dribl.api

class DriblResponseException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
