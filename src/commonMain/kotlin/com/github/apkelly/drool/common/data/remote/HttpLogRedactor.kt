package com.github.apkelly.drool.common.data.remote

private val sensitiveJsonValue = Regex(
    pattern = """(?i)("(?:password|password_confirmation|access_token|refresh_token|token|jwt|""" +
        """authToken|bearerToken|authorization|cookie|set-cookie|client_secret)"\s*:\s*)""" +
        """"(?:\\.|[^"\\])*"""",
)

/**
 * Replaces credential-like JSON string values before an HTTP message reaches application logs.
 *
 * Field names and JSON structure remain visible for diagnostics, while passwords, tokens,
 * authorization values, cookies, and client secrets are never written in plaintext.
 */
internal fun redactSensitiveHttpLogMessage(
    message: String,
): String = sensitiveJsonValue.replace(message) { match ->
    "${match.groupValues[1]}\"██\""
}
