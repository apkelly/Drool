package com.github.apkelly.drool.common.data.repository

import com.github.apkelly.dribl.api.AuthRemoteDataSource
import com.github.apkelly.dribl.api.DriblHttpException
import com.github.apkelly.dribl.api.DriblResponseException
import com.github.apkelly.dribl.api.SportsRemoteDataSource
import com.github.apkelly.dribl.auth.profileFromBearerToken
import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.local.inTransaction
import com.github.apkelly.drool.common.data.mapper.accountEmailUpdates
import com.github.apkelly.drool.common.data.mapper.emailAddress
import com.github.apkelly.drool.common.data.mapper.linkedIdentityUpdates
import com.github.apkelly.drool.common.data.mapper.reconcileRelatedUsers
import com.github.apkelly.drool.common.data.mapper.toCurrentProfile
import com.github.apkelly.drool.common.data.mapper.toDomain
import com.github.apkelly.drool.common.data.mapper.toDriblRequest
import com.github.apkelly.drool.common.data.mapper.toEntity
import com.github.apkelly.drool.common.data.mapper.toLinkedEntity
import com.github.apkelly.drool.common.data.mapper.withIdentityFrom
import com.github.apkelly.drool.common.data.repository.SessionDataSource
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.domain.model.Account
import com.github.apkelly.drool.common.domain.model.AuthCredentials
import com.github.apkelly.drool.common.domain.model.AuthenticatedSession
import com.github.apkelly.drool.common.domain.model.AuthenticationException
import com.github.apkelly.drool.common.domain.model.AuthenticationFailure
import com.github.apkelly.drool.common.domain.model.LinkMemberException
import com.github.apkelly.drool.common.domain.model.LinkMemberFailure
import com.github.apkelly.drool.common.domain.model.LinkMemberRequestResult
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.linkMemberFailureForHttpStatus
import com.github.apkelly.drool.common.logging.DroolLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDataSourceImpl(
    private val authApi: AuthRemoteDataSource,
    private val sportsApi: SportsRemoteDataSource,
    private val store: BearerTokenStore,
    private val database: DroolDatabase,
) : SessionDataSource {
    private val logger = DroolLog.withTag("SessionDataSource")

    override fun observeProfile() = database.profileDao().observeActive().flatMapLatest { profile ->
        if (profile == null) {
            flowOf(null)
        } else {
            combine(
                database.profileDao().observeRelatedUsers(profile.accountId),
                database.profileDao().observeAccounts(profile.accountId),
            ) { users, accounts ->
                profile.toDomain(
                    relatedUsers = users.map { it.toDomain() },
                    accounts = accounts.map { it.toDomain() },
                )
            }
        }
    }

    override suspend fun restoreSession(): AuthenticatedSession? {
        val token = store.getBearerToken() ?: return null
        return try {
            val cachedProfile = database.profileDao().getActive()?.let { profile ->
                profile.toDomain(
                    relatedUsers = database.profileDao().getRelatedUsers(profile.accountId)
                        .map { it.toDomain() },
                    accounts = database.profileDao().getAccounts(profile.accountId)
                        .map { it.toDomain() },
                )
            }
            val tokenProfile = profileFromBearerToken(token)?.toDomain()
            if (cachedProfile != null) {
                val repairedProfile = tokenProfile?.takeIf {
                    cachedProfile.displayName.needsRepair(cachedProfile.accountId) &&
                        it.accountId == cachedProfile.accountId &&
                        !it.displayName.needsRepair(it.accountId)
                }
                if (repairedProfile != null) {
                    persistProfile(repairedProfile)
                    refreshAssociations(
                        token,
                        repairedProfile.accountId,
                        repairedProfile.email ?: cachedProfile.email,
                        rejectUnauthorized = true,
                    )
                    return AuthenticatedSession(
                        token,
                        attachAssociations(repairedProfile),
                    )
                }
                refreshAssociations(
                    token,
                    cachedProfile.accountId,
                    cachedProfile.email,
                    rejectUnauthorized = true,
                )
                return AuthenticatedSession(
                    token,
                    attachAssociations(cachedProfile),
                )
            }

            val profileDto = tokenProfile ?: sportsApi.fetchProfile(token).toDomain()
            persistProfile(profileDto)
            refreshAssociations(
                token,
                profileDto.accountId,
                profileDto.email,
                rejectUnauthorized = true,
            )
            AuthenticatedSession(
                token,
                attachAssociations(profileDto),
            )
        } catch (error: DriblHttpException) {
            if (error.statusCode == HTTP_UNAUTHORIZED || error.statusCode == HTTP_FORBIDDEN) {
                store.clearBearerToken()
                null
            } else {
                throw error
            }
        }
    }

    override suspend fun signIn(
        credentials: AuthCredentials,
    ): AuthenticatedSession {
        try {
            val authSession = authApi.signIn(credentials.toDriblRequest()).toDomain()
            val token = authSession.bearerToken
            val resolvedProfile = resolveProfile(token)
            val firstName = authSession.firstName?.trim().orEmpty()
            val displayName = when {
                firstName.isNotEmpty() -> firstName
                resolvedProfile.displayName == resolvedProfile.accountId -> credentials.username.trim()
                else -> resolvedProfile.displayName
            }
            val profileDto = resolvedProfile.copy(
                displayName = displayName,
                email = authSession.identity?.email
                    ?: resolvedProfile.email
                    ?: credentials.username.trim().takeIf { it.looksLikeEmail() },
                avatarUrl = authSession.identity?.avatarUrl ?: resolvedProfile.avatarUrl,
            )
            persistProfile(
                profileDto = profileDto,
                relatedUsers = authSession.relatedUsers,
                accounts = authSession.accounts,
                replaceAssociations = true,
            )
            refreshAssociations(token, profileDto.accountId, credentials.username.trim())
            store.saveBearerToken(token)
            return AuthenticatedSession(
                token,
                attachAssociations(profileDto),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: DriblHttpException) {
            val reason = if (error.statusCode == 401 || error.statusCode == 403) {
                AuthenticationFailure.InvalidCredentials
            } else {
                AuthenticationFailure.Unknown
            }
            authenticationFailed(reason, error)
        } catch (error: IOException) {
            authenticationFailed(AuthenticationFailure.Offline, error)
        } catch (error: DriblResponseException) {
            authenticationFailed(AuthenticationFailure.Unknown, error)
        } catch (error: SerializationException) {
            authenticationFailed(AuthenticationFailure.Unknown, error)
        } catch (unexpected: Throwable) {
            authenticationFailed(AuthenticationFailure.Unknown, unexpected)
        }
    }

    private suspend fun resolveProfile(
        token: String,
    ): Profile {
        val tokenProfile = profileFromBearerToken(token)
        return if (tokenProfile == null) {
            sportsApi.fetchProfile(token).toDomain()
        } else {
            tokenProfile.toDomain()
        }
    }

    private suspend fun refreshAssociations(
        token: String,
        accountId: String,
        relatedUsersEmail: String?,
        rejectUnauthorized: Boolean = false,
    ) {
        val accountEmail = refreshAccounts(token, accountId, rejectUnauthorized)
        val email = relatedUsersEmail ?: accountEmail
        if (email == null) {
            logger.w { "Unable to refresh related users; profile has no email identifier" }
        } else {
            refreshRelatedUsers(token, accountId, email, rejectUnauthorized)
        }
        refreshLinkedUsers(token, accountId, rejectUnauthorized)
    }

    override suspend fun refreshProfileEndpoint(
        endpoint: ProfileApiEndpoint,
        email: String,
    ) {
        val token = store.getBearerToken()
        if (token == null) {
            logger.w { "Unable to run profile API diagnostic; no bearer token is stored" }
            return
        }
        val accountId = store.getActiveAccountId()
        if (accountId == null) {
            logger.w { "Unable to run profile API diagnostic; no active account is stored" }
            return
        }
        when (endpoint) {
            ProfileApiEndpoint.RelatedUsers -> refreshRelatedUsers(token, accountId, email)
            ProfileApiEndpoint.AccessAccounts -> refreshAccounts(token, accountId)
            ProfileApiEndpoint.LinkedUsers -> refreshLinkedUsers(token, accountId)
        }
    }

    override suspend fun getLinkCandidates(): List<RelatedUser> {
        val token = requireBearerToken()
        val accountId = requireActiveAccountId()
        return linkMemberRequest {
            sportsApi.fetchLinkCandidates(token).map { it.toDomain() }
        }.filter { it.subjectUserId != accountId }
    }

    override suspend fun requestLinkedUser(
        candidateId: String,
        isGuardian: Boolean,
    ): LinkMemberRequestResult = linkMemberRequest {
        val token = requireBearerToken()
        val accountId = requireActiveAccountId()
        val result = sportsApi.requestLinkedUser(token, candidateId, isGuardian)
        if (result.isApproved) {
            refreshLinkedUsers(token, accountId)
        }
        LinkMemberRequestResult(
            relationshipId = result.relationshipId,
            isApproved = result.isApproved,
        )
    }

    override suspend fun verifyLinkedUser(
        candidateId: String,
        token: String,
    ) {
        linkMemberRequest(invalidTokenOnClientError = true) {
            val bearerToken = requireBearerToken()
            sportsApi.verifyLinkedUser(bearerToken, candidateId, token)
            val accountId = requireActiveAccountId()
            refreshLinkedUsers(bearerToken, accountId)
        }
    }

    override suspend fun unlinkLinkedUser(
        linkId: String,
    ) {
        linkMemberRequest {
            val bearerToken = requireBearerToken()
            val accountId = requireActiveAccountId()
            sportsApi.unlinkLinkedUser(bearerToken, linkId)
            database.profileDao().markRelatedUserUnlinked(accountId, linkId)
            refreshLinkedUsers(bearerToken, accountId)
        }
    }

    private suspend fun requireBearerToken(): String = store.getBearerToken()
        ?: throw LinkMemberException(LinkMemberFailure.SessionExpired)

    private suspend fun requireActiveAccountId(): String = store.getActiveAccountId()
        ?: throw LinkMemberException(LinkMemberFailure.SessionExpired)

    private suspend fun refreshRelatedUsers(
        token: String,
        accountId: String,
        email: String,
        rejectUnauthorized: Boolean = false,
    ) {
        refreshAssociation("related users", rejectUnauthorized) {
            val users = sportsApi.fetchRelatedUsers(token, email).map { it.toDomain() }
            database.inTransaction {
                database.profileDao().getActive()
                    ?.takeIf { it.accountId == accountId }
                    ?.let { database.profileDao().upsert(it.withIdentityFrom(users, email)) }
                val linkedUsers = database.profileDao().getRelatedUsers(accountId)
                    .filter { it.isLinked }
                val reconciled = reconcileRelatedUsers(
                    relatedUsers = users.map { it.toEntity(accountId) },
                    linkedUsers = linkedUsers,
                )
                database.profileDao().deleteRelatedUsers(accountId)
                database.profileDao().upsertRelatedUsers(reconciled)
            }
        }
    }

    private suspend fun refreshLinkedUsers(
        token: String,
        accountId: String,
        rejectUnauthorized: Boolean = false,
    ) {
        refreshAssociation("linked users", rejectUnauthorized) {
            val users = sportsApi.fetchLinkedUsers(token).map { it.toDomain() }
            database.inTransaction {
                database.profileDao().getActive()
                    .linkedIdentityUpdates(accountId, users)
                    .forEach {
                        database.profileDao().upsert(it)
                    }
                val reconciled = reconcileRelatedUsers(
                    relatedUsers = database.profileDao().getRelatedUsers(accountId),
                    linkedUsers = users.map { it.toLinkedEntity(accountId) },
                )
                database.profileDao().deleteRelatedUsers(accountId)
                database.profileDao().upsertRelatedUsers(reconciled)
            }
        }
    }

    private suspend fun refreshAccounts(
        token: String,
        accountId: String,
        rejectUnauthorized: Boolean = false,
    ): String? {
        var accountEmail: String? = null
        refreshAssociation("accounts", rejectUnauthorized) {
            val accounts = sportsApi.fetchAccounts(token).map { it.toDomain() }
            accountEmail = accounts.emailAddress()
            database.inTransaction {
                database.profileDao().getActive()
                    .accountEmailUpdates(accountId, accounts)
                    .forEach {
                        database.profileDao().upsert(it)
                    }
                database.profileDao().deleteAccounts(accountId)
                database.profileDao().upsertAccounts(accounts.map { it.toEntity(accountId) })
            }
        }
        return accountEmail
    }

    private suspend fun refreshAssociation(
        name: String,
        rejectUnauthorized: Boolean,
        refresh: suspend () -> Unit,
    ) {
        try {
            refresh()
        } catch (error: CancellationException) {
            throw error
        } catch (error: DriblHttpException) {
            if (
                rejectUnauthorized &&
                (error.statusCode == HTTP_UNAUTHORIZED || error.statusCode == HTTP_FORBIDDEN)
            ) {
                throw error
            }
            logger.w {
                "Unable to refresh $name; preserving cached data (${error::class.simpleName})"
            }
        } catch (unexpected: Exception) {
            logger.w {
                "Unable to refresh $name; preserving cached data (${unexpected::class.simpleName})"
            }
        }
    }

    private suspend fun <T> linkMemberRequest(
        invalidTokenOnClientError: Boolean = false,
        block: suspend () -> T,
    ): T = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: LinkMemberException) {
        throw error
    } catch (error: DriblHttpException) {
        throw LinkMemberException(
            reason = linkMemberFailureForHttpStatus(
                error.statusCode,
                invalidTokenOnClientError,
            ),
            cause = error,
        )
    } catch (error: IOException) {
        throw LinkMemberException(LinkMemberFailure.Offline, error)
    } catch (unexpected: Exception) {
        throw LinkMemberException(LinkMemberFailure.Unknown, unexpected)
    }

    private suspend fun attachAssociations(
        profile: Profile,
    ): Profile {
        val current = database.profileDao().getActive().toCurrentProfile(profile)
        return current.copy(
            relatedUsers = database.profileDao().getRelatedUsers(current.accountId)
                .map { it.toDomain() },
            accounts = database.profileDao().getAccounts(current.accountId)
                .map { it.toDomain() },
        )
    }

    private suspend fun persistProfile(
        profileDto: Profile,
        relatedUsers: List<RelatedUser> = emptyList(),
        accounts: List<Account> = emptyList(),
        replaceAssociations: Boolean = false,
    ) {
        database.inTransaction {
            database.profileDao().deactivateAll()
            database.profileDao().upsert(profileDto.toEntity())
            if (replaceAssociations) {
                database.profileDao().deleteRelatedUsers(profileDto.accountId)
                database.profileDao().deleteAccounts(profileDto.accountId)
                database.profileDao().upsertRelatedUsers(
                    relatedUsers.map { it.toEntity(profileDto.accountId) },
                )
                database.profileDao().upsertAccounts(
                    accounts.map { it.toEntity(profileDto.accountId) },
                )
            }
        }
        store.setActiveAccountId(profileDto.accountId)
    }

    override suspend fun signOut() {
        store.clearBearerToken()
        database.inTransaction {
            database.cacheMetadataDao().deleteAll()
            database.fixtureDao().deleteAll()
            database.refereeAssignmentSnapshotDao().deleteAll()
            database.teamDao().deleteAllRelationships()
            database.clubDao().deleteAllRelationships()
            database.profileDao().deleteAllRelatedUsers()
            database.profileDao().deleteAllAccounts()
            database.profileDao().deleteAllProfiles()
            database.teamDao().deleteAll()
            database.clubDao().deleteAll()
            database.teamHubCacheDao().deleteAll()
            database.matchWeatherCacheDao().deleteAll()
        }
    }

    private fun String.needsRepair(
        accountId: String,
    ): Boolean = this == accountId || looksLikeEmail()

    private fun String.looksLikeEmail(): Boolean = contains('@')

    private fun authenticationFailed(
        reason: AuthenticationFailure,
        cause: Throwable,
    ): Nothing = throw AuthenticationException(reason, cause)

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val LINK_CANDIDATE_SCOPE = "link-candidate"
    }
}
