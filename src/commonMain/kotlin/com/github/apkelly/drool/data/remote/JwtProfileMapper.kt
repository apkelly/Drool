package com.github.apkelly.drool.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import com.github.apkelly.drool.domain.model.Profile

internal fun profileFromBearerToken(token: String): Profile? {
    val payload = token.split('.').takeIf { it.size == 3 }?.get(1) ?: return null
    val claims = runCatching {
        networkJson.parseToJsonElement(payload.decodeBase64Url().decodeToString()) as? JsonObject
    }.getOrNull() ?: return null
    val accountId = claims.claim("account_id", "accountId", "user_id", "userId", "sub", "id")
        ?: return null
    val email = claims.claim("email", "email_address", "emailAddress")
    val explicitDisplayName = claims.claim(
        "display_name",
        "displayName",
        "full_name",
        "fullName",
        "name",
        "username",
        "user_name",
        "userName",
        "preferred_username",
        "preferredUsername",
        "login",
    )
    val firstName = claims.claim("first_name", "firstName", "given_name", "givenName")
    val composedName = listOfNotNull(
        firstName,
        claims.claim("last_name", "lastName", "family_name", "familyName"),
    ).joinToString(" ").takeIf { it.isNotBlank() }
    val displayName = firstName ?: explicitDisplayName ?: composedName ?: email ?: accountId
    return Profile(
        accountId = accountId,
        displayName = displayName,
        email = email,
        playingTeamIds = emptySet(),
        avatarUrl = claims.claim(
            "avatar",
            "avatar_url",
            "avatarUrl",
            "profile_image",
            "profile_image_url",
            "profileImage",
            "profileImageUrl",
            "picture",
        ),
    )
}

private fun JsonObject.claim(vararg keys: String): String? {
    keys.forEach { key ->
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }?.let {
            return it
        }
    }
    values.forEach { value ->
        if (value is JsonObject) {
            value.claim(*keys)?.let { return it }
        }
    }
    return null
}

private fun String.decodeBase64Url(): ByteArray {
    val output = ArrayList<Byte>((length * 3) / 4)
    var buffer = 0
    var bits = 0
    for (character in this) {
        if (character == '=') break
        val value = when (character) {
            in 'A'..'Z' -> character - 'A'
            in 'a'..'z' -> character - 'a' + 26
            in '0'..'9' -> character - '0' + 52
            '-', '+' -> 62
            '_', '/' -> 63
            else -> throw IllegalArgumentException("Invalid Base64URL character")
        }
        buffer = (buffer shl 6) or value
        bits += 6
        if (bits >= 8) {
            bits -= 8
            output += ((buffer shr bits) and 0xFF).toByte()
        }
    }
    return output.toByteArray()
}
