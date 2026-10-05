package com.github.apkelly.drool.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import com.github.apkelly.drool.data.remote.dto.PersonDto
import com.github.apkelly.drool.data.remote.dto.SignInResponse
import com.github.apkelly.drool.data.remote.model.AuthApiRequest
import com.github.apkelly.drool.domain.model.AuthenticationIdentity
import com.github.apkelly.drool.domain.model.AuthenticationResult

class DriblApi(private val client: HttpClient) : AuthRemoteDataSource {
    override suspend fun signIn(request: AuthApiRequest): AuthenticationResult {
        val response = client.post(request.url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.UserAgent, OriginalDriblUserAgent)
            setBody(
                buildJsonObject {
                    put(request.usernameField, request.username)
                    put(request.passwordField, request.password)
                }
            )
        }.requireBody<SignInResponse>(request.url)

        return AuthenticationResult(
            bearerToken = response.token,
            identity = response.user.toIdentityDto(),
        )
    }
}

private fun PersonDto.toIdentityDto() =
    AuthenticationIdentity(
        id = (userId ?: accountId ?: id).textValue,
        firstName = firstName,
        lastName = lastName,
        email = email
            ?: primaryEmail
            ?: sendingEmailAddress
            ?: emailAddress
            ?: value?.takeIf { it.contains('@') },
        avatarUrl = avatarUrl ?: profileImage ?: image ?: systemImage ?: source,
    )

private val kotlinx.serialization.json.JsonPrimitive?.textValue: String?
    get() = this?.contentOrNull?.takeUnless { it.isBlank() || it == "null" }
