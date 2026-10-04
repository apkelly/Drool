package com.github.apkelly.drool.data.remote

private val sensitiveJsonValue = Regex(
    pattern = """(?i)("(?:password|password_confirmation|access_token|refresh_token|token|jwt|authToken|bearerToken|authorization|cookie|set-cookie|client_secret)"\s*:\s*)"(?:\\.|[^"\\])*"""",
)

internal fun redactSensitiveHttpLogMessage(message: String): String = message

//internal fun redactSensitiveHttpLogMessage(message: String): String =
//    sensitiveJsonValue.replace(message) { match ->
//        "${match.groupValues[1]}\"██\""
//    }
