package com.github.apkelly.dribl.api

import com.github.apkelly.dribl.dto.AccountDto
import com.github.apkelly.dribl.dto.EmergencyContactResourceDto
import com.github.apkelly.dribl.dto.ImpersonationResponse
import com.github.apkelly.dribl.dto.LinkUserRequest
import com.github.apkelly.dribl.dto.LinkUserSelection
import com.github.apkelly.dribl.dto.MemberCardsResponse
import com.github.apkelly.dribl.dto.PersonDto
import com.github.apkelly.dribl.dto.PersonResourceDto
import com.github.apkelly.dribl.dto.ProfileResponse
import com.github.apkelly.dribl.dto.ResourceList
import com.github.apkelly.dribl.dto.ShortcutResponse
import com.github.apkelly.dribl.dto.TokenVerificationRequest
import com.github.apkelly.dribl.dto.UserResponse
import com.github.apkelly.dribl.mapper.accountFromDto
import com.github.apkelly.dribl.mapper.avatar
import com.github.apkelly.dribl.mapper.displayName
import com.github.apkelly.dribl.mapper.emergencyContactFromDto
import com.github.apkelly.dribl.mapper.identifier
import com.github.apkelly.dribl.mapper.profile
import com.github.apkelly.dribl.mapper.relatedUserFromPerson
import com.github.apkelly.dribl.mapper.relatedUserFromResource
import com.github.apkelly.dribl.mapper.toDomain
import com.github.apkelly.dribl.model.Account
import com.github.apkelly.dribl.model.Club
import com.github.apkelly.dribl.model.EmergencyContact
import com.github.apkelly.dribl.model.LinkMemberRequestResult
import com.github.apkelly.dribl.model.Profile
import com.github.apkelly.dribl.model.RelatedUser
import com.github.apkelly.dribl.model.TeamAssociation
import com.github.apkelly.dribl.model.TeamRelationship
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal class DriblProfileApi(
    private val transport: DriblHttpTransport,
) {
    suspend fun fetchProfile(
        bearerToken: String,
    ): Profile {
        val response = transport.getBody<ProfileResponse>("/auth/related-users", bearerToken)
        val profile = response.profile()
            ?: throw DriblResponseException("Profile response did not contain a profile")
        val accountId = profile.identifier()
            ?: throw DriblResponseException(
                "Profile response did not contain an account identifier",
            )
        return Profile(
            accountId = accountId,
            displayName = profile.displayName(accountId),
            email = profile.email ?: profile.emailAddress,
            avatarUrl = profile.avatar(),
        )
    }

    suspend fun fetchRelatedUsers(
        bearerToken: String,
        email: String,
    ): List<RelatedUser> {
        val response = transport.getBody<ResourceList<PersonDto>>(
            "/auth/related-users",
            bearerToken,
        ) {
            parameter("email", email)
        }
        return response.items.mapIndexedNotNull { index, person ->
            relatedUserFromPerson(index, person, isLinked = false)
        }
    }

    suspend fun fetchAccounts(
        bearerToken: String,
    ): List<Account> {
        val response = transport.getBody<ResourceList<AccountDto>>(
            "/access/accounts",
            bearerToken,
        )
        return response.items.mapIndexed(::accountFromDto)
    }

    suspend fun fetchLinkedUsers(
        bearerToken: String,
    ): List<RelatedUser> {
        val response = transport.getBody<ResourceList<PersonResourceDto>>(
            "/linked-users",
            bearerToken,
        )
        return response.items
            .filter {
                it.attributes.status == null ||
                    it.attributes.status.equals("approved", ignoreCase = true)
            }
            .mapIndexedNotNull { index, resource ->
                relatedUserFromResource(index, resource, isLinked = true)
            }
    }

    suspend fun fetchLinkCandidates(
        bearerToken: String,
    ): List<RelatedUser> {
        val response = transport.getBody<ResourceList<PersonDto>>(
            "/linked-users-lookup",
            bearerToken,
        )
        return response.items.mapIndexed { index, person ->
            relatedUserFromPerson(index, person)
        }
    }

    suspend fun requestLinkedUser(
        bearerToken: String,
        candidateId: String,
        isGuardian: Boolean,
    ): LinkMemberRequestResult {
        val url = transport.url("/linked-users")
        val response = transport.client.post(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(
                LinkUserRequest(
                    relatedUser = LinkUserSelection(candidateId, isGuardian),
                    approve = true,
                ),
            )
        }.requireBody<UserResponse>(url)
        val linkedUser = response.data
            ?: throw DriblResponseException("Link user response did not contain relationship data")
        val relationshipId = linkedUser.id.textValue
            ?: throw DriblResponseException("Link user response did not contain a relationship ID")
        return LinkMemberRequestResult(
            relationshipId = relationshipId,
            isApproved = linkedUser.attributes.status.equals("approved", ignoreCase = true),
        )
    }

    suspend fun verifyLinkedUser(
        bearerToken: String,
        candidateId: String,
        token: String,
    ) {
        val url = transport.url("/linked-users/$candidateId")
        transport.client.patch(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(TokenVerificationRequest(token))
        }.requireSuccess(url)
    }

    suspend fun unlinkLinkedUser(
        bearerToken: String,
        linkId: String,
    ) {
        val url = transport.url("/linked-users/$linkId")
        transport.client.delete(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
        }.requireSuccess(url)
    }

    suspend fun createProfileSession(
        bearerToken: String,
        userId: String,
    ): String {
        val url = transport.url("/auth/impersonate/$userId")
        val response = transport.client.post(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
        }.requireBody<ImpersonationResponse>(url)
        return response.token
            ?: throw DriblResponseException(
                "Profile session response did not contain token",
            )
    }

    suspend fun fetchPersonalInformation(
        bearerToken: String,
        userId: String,
    ): RelatedUser {
        val response = transport.getBody<UserResponse>("/users/$userId", bearerToken)
        val user = response.data
            ?: throw DriblResponseException(
                "Personal information response did not contain a user",
            )
        return relatedUserFromResource(0, user, isLinked = true)
    }

    suspend fun fetchEmergencyContacts(
        bearerToken: String,
    ): List<EmergencyContact> {
        val response = transport.getBody<ResourceList<EmergencyContactResourceDto>>(
            "/user-contacts/",
            bearerToken,
        )
        return response.items.mapIndexedNotNull { index, resource ->
            emergencyContactFromDto(index, resource.attributes)
        }
    }

    suspend fun fetchProfileTeams(
        bearerToken: String,
    ): List<TeamAssociation> {
        val response = transport.getBody<ShortcutResponse>("/access/shortcut", bearerToken) {
            parameter("status", "active")
            parameter("require_color", true)
        }
        return response.teams.mapIndexedNotNull { index, resource ->
            val attributes = resource.attributes
            val team = attributes.copy(id = resource.id).toDomain(index)
                ?: return@mapIndexedNotNull null
            val roleSlugs = (attributes.roles + resource.roles)
                .flatMap {
                    listOfNotNull(
                        it.attributes.slug?.lowercase(),
                        it.attributes.name?.lowercase(),
                    )
                }
            TeamAssociation(
                team = team,
                relationship = roleSlugs.toTeamRelationship(),
            )
        }
    }

    suspend fun fetchProfileClubs(
        bearerToken: String,
    ): List<Club> {
        val response = transport.getBody<MemberCardsResponse>(
            "/universal/member-cards",
            bearerToken,
        )
        return response.teams.mapIndexedNotNull { index, resource ->
            val attributes = resource.attributes
            val name = attributes.clubName ?: return@mapIndexedNotNull null
            Club(
                id = attributes.clubId.textValue ?: "club-$index-$name",
                name = name,
                shortName = null,
                logoUrl = attributes.image,
                primaryColor = attributes.color,
                secondaryColor = attributes.accent,
            )
        }.distinctBy(Club::id)
    }

    private suspend inline fun <reified T> getBody(
        path: String,
        bearerToken: String,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val url = transport.url(path)
        return transport.client.get(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            configure()
        }.requireBody(url)
    }
}

private fun List<String>.toTeamRelationship(): TeamRelationship = when {
    any { "manager" in it } -> TeamRelationship.Managing
    any { "coach" in it } -> TeamRelationship.Coaching
    any { "player" in it } -> TeamRelationship.Player
    any { "supporter" in it } -> TeamRelationship.Following
    else -> TeamRelationship.None
}

private val JsonPrimitive?.textValue: String?
    get() = this?.contentOrNull?.takeUnless { it.isBlank() || it == "null" }
