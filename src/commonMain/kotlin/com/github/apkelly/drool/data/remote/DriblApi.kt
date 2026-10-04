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
import com.github.apkelly.drool.data.remote.dto.AccountDto
import com.github.apkelly.drool.data.remote.dto.PersonDto
import com.github.apkelly.drool.data.remote.dto.SignInResponse
import com.github.apkelly.drool.data.remote.model.AuthApiRequest
import com.github.apkelly.drool.domain.model.Account
import com.github.apkelly.drool.domain.model.AuthenticationIdentity
import com.github.apkelly.drool.domain.model.AuthenticationResult
import com.github.apkelly.drool.domain.model.RelatedUser

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

        val payload = response.data
        val token = payload?.accessToken ?: response.accessToken
            ?: throw DriblResponseException(
                "Sign-in succeeded but access_token was missing"
            )
        val identity = payload?.user

        return AuthenticationResult(
            bearerToken = token,
            identity = identity?.toIdentityDto(),
            relatedUsers = payload?.relatedUsers.orEmpty()
                .mapIndexed(::relatedUserFromDto),
            accounts = payload?.accounts.orEmpty()
                .mapIndexed(::accountFromDto),
        )
    }
}

private fun PersonDto.toIdentityDto() =
    AuthenticationIdentity(
        id = (userId ?: accountId ?: id).textValue,
        firstName = firstName,
        lastName = lastName,
        email = email ?: emailAddress ?: value?.takeIf { it.contains('@') },
        avatarUrl = avatarUrl ?: profileImage ?: image ?: source,
    )

private fun relatedUserFromDto(index: Int, user: PersonDto): RelatedUser {
    val email = user.email
        ?: user.emailAddress
        ?: user.value?.takeIf { it.contains('@') }
    val id = (user.userId
            ?: user.accountId
            ?: user.id).textValue
            ?: "related-$index-${email ?: user.firstName.orEmpty()}"
    val displayName = listOfNotNull(user.firstName, user.lastName)
        .joinToString(" ")
        .takeIf(String::isNotBlank)
        ?: email
        ?: id
    return RelatedUser(
        id = id,
        displayName = displayName,
        email = email,
        avatarUrl = user.avatarUrl ?: user.profileImage ?: user.image ?: user.source,
        subjectUserId = id,
        dateOfBirth = user.dateOfBirth ?: user.alternateDateOfBirth,
    )
}

private fun accountFromDto(index: Int, account: AccountDto): Account {
    val id = (account.accountId ?: account.id).textValue ?: "account-$index"
    return Account(
        id = id,
        name = account.accountName ?: account.name ?: account.value ?: id,
        subtitle = account.role ?: account.type,
        logoUrl = account.logoUrl ?: account.image,
    )
}

private val kotlinx.serialization.json.JsonPrimitive?.textValue: String?
    get() = this?.contentOrNull?.takeUnless { it.isBlank() || it == "null" }
