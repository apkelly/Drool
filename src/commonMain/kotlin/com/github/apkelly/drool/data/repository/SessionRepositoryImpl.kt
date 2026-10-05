package com.github.apkelly.drool.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import com.github.apkelly.drool.data.local.DroolDatabase
import com.github.apkelly.drool.data.local.inTransaction
import com.github.apkelly.drool.data.local.entity.TeamRelationshipEntity
import com.github.apkelly.drool.data.mapper.toDomain
import com.github.apkelly.drool.data.mapper.toCurrentProfile
import com.github.apkelly.drool.data.mapper.toEntity
import com.github.apkelly.drool.data.mapper.toLinkedEntity
import com.github.apkelly.drool.data.mapper.withIdentityFrom
import com.github.apkelly.drool.data.mapper.linkedIdentityUpdates
import com.github.apkelly.drool.data.mapper.accountEmailUpdates
import com.github.apkelly.drool.data.mapper.emailAddress
import com.github.apkelly.drool.data.mapper.reconcileRelatedUsers
import com.github.apkelly.drool.data.remote.AuthRemoteDataSource
import com.github.apkelly.drool.data.remote.DriblHttpException
import com.github.apkelly.drool.data.remote.DriblResponseException
import com.github.apkelly.drool.data.remote.SportsRemoteDataSource
import com.github.apkelly.drool.data.remote.profileFromBearerToken
import com.github.apkelly.drool.data.remote.model.toApiRequest
import com.github.apkelly.drool.data.storage.BearerTokenStore
import com.github.apkelly.drool.domain.model.AuthCredentials
import com.github.apkelly.drool.domain.model.Account
import com.github.apkelly.drool.domain.model.AuthenticatedSession
import com.github.apkelly.drool.domain.model.AuthenticationException
import com.github.apkelly.drool.domain.model.AuthenticationFailure
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.domain.model.LinkMemberException
import com.github.apkelly.drool.domain.model.LinkMemberFailure
import com.github.apkelly.drool.domain.model.linkMemberFailureForHttpStatus
import com.github.apkelly.drool.domain.model.RelatedUser
import com.github.apkelly.drool.domain.model.TeamRelationship
import com.github.apkelly.drool.domain.repository.SessionRepository
import com.github.apkelly.drool.logging.DroolLog

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryImpl(
    private val authApi: AuthRemoteDataSource,
    private val sportsApi: SportsRemoteDataSource,
    private val store: BearerTokenStore,
    private val database: DroolDatabase,
) : SessionRepository {
    private val logger = DroolLog.withTag("SessionRepository")

    override fun observeProfile() =
        database.profileDao().observeActive().flatMapLatest { profile ->
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
        val cachedProfile = database.profileDao().getActive()?.let { profile ->
            profile.toDomain(
                relatedUsers = database.profileDao().getRelatedUsers(profile.accountId)
                    .map { it.toDomain() },
                accounts = database.profileDao().getAccounts(profile.accountId)
                    .map { it.toDomain() },
            )
        }
        val tokenProfile = profileFromBearerToken(token)
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
                )
                return AuthenticatedSession(
                    token,
                    attachAssociations(repairedProfile),
                )
            }
            refreshAssociations(token, cachedProfile.accountId, cachedProfile.email)
            return AuthenticatedSession(
                token,
                attachAssociations(cachedProfile),
            )
        }

        return try {
            val profileDto = tokenProfile ?: sportsApi.fetchProfile(token)
            persistProfile(profileDto)
            refreshAssociations(token, profileDto.accountId, profileDto.email)
            AuthenticatedSession(
                token,
                attachAssociations(profileDto),
            )
        } catch (error: DriblHttpException) {
            if (error.statusCode == 401 || error.statusCode == 403) {
                store.clearBearerToken()
                null
            } else {
                throw error
            }
        }
    }

    override suspend fun signIn(credentials: AuthCredentials): AuthenticatedSession {
        try {
            val authSession = authApi.signIn(credentials.toApiRequest())
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
            throw AuthenticationException(reason, error)
        } catch (error: IOException) {
            throw AuthenticationException(AuthenticationFailure.Offline, error)
        } catch (error: DriblResponseException) {
            throw AuthenticationException(AuthenticationFailure.Unknown, error)
        } catch (error: SerializationException) {
            throw AuthenticationException(AuthenticationFailure.Unknown, error)
        } catch (error: Throwable) {
            throw AuthenticationException(AuthenticationFailure.Unknown, error)
        }
    }

    private suspend fun resolveProfile(token: String): Profile =
        profileFromBearerToken(token) ?: sportsApi.fetchProfile(token)

    private suspend fun refreshAssociations(
        token: String,
        accountId: String,
        relatedUsersEmail: String?,
    ) {
        val accountEmail = refreshAccounts(token, accountId)
        val email = relatedUsersEmail ?: accountEmail
        if (email == null) {
            logger.w { "Unable to refresh related users; profile has no email identifier" }
        } else {
            refreshRelatedUsers(token, accountId, email)
        }
        refreshLinkedUsers(token, accountId)
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

    override suspend fun getLinkCandidates(): List<RelatedUser> =
        linkMemberRequest {
            val token = store.getBearerToken()
                ?: throw LinkMemberException(LinkMemberFailure.SessionExpired)
            sportsApi.fetchLinkCandidates(token)
        }

    override suspend fun verifyLinkedUser(candidateId: String, token: String) {
        linkMemberRequest(invalidTokenOnClientError = true) {
            val bearerToken = store.getBearerToken()
                ?: throw LinkMemberException(LinkMemberFailure.SessionExpired)
            sportsApi.verifyLinkedUser(bearerToken, candidateId, token)
            val accountId = store.getActiveAccountId()
                ?: throw LinkMemberException(LinkMemberFailure.SessionExpired)
            refreshLinkedUsers(bearerToken, accountId)
        }
    }

    private suspend fun refreshRelatedUsers(
        token: String,
        accountId: String,
        email: String,
    ) {
        refreshAssociation("related users") {
            val users = sportsApi.fetchRelatedUsers(token, email)
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

    private suspend fun refreshLinkedUsers(token: String, accountId: String) {
        refreshAssociation("linked users") {
            val users = sportsApi.fetchLinkedUsers(token)
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

    private suspend fun refreshAccounts(token: String, accountId: String): String? {
        var accountEmail: String? = null
        refreshAssociation("accounts") {
            val accounts = sportsApi.fetchAccounts(token)
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

    private suspend fun refreshAssociation(name: String, refresh: suspend () -> Unit) {
        try {
            refresh()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.w {
                "Unable to refresh $name; preserving cached data (${error::class.simpleName})"
            }
        }
    }

    private suspend fun <T> linkMemberRequest(
        invalidTokenOnClientError: Boolean = false,
        block: suspend () -> T,
    ): T =
        try {
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
        } catch (error: Exception) {
            throw LinkMemberException(LinkMemberFailure.Unknown, error)
        }

    private suspend fun attachAssociations(profile: Profile): Profile {
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
        val playingRelationships = profileDto.playingTeamIds.map { teamId ->
            TeamRelationshipEntity(
                accountId = profileDto.accountId,
                teamId = teamId,
                relationship = TeamRelationship.PlaysFor.name,
            )
        }
        database.inTransaction {
            database.profileDao().deactivateAll()
            database.profileDao().upsert(profileDto.toEntity())
            playingRelationships.forEach { database.teamDao().upsertRelationship(it) }
            if (replaceAssociations) {
                database.profileDao().deleteRelatedUsers(profileDto.accountId)
                database.profileDao().deleteAccounts(profileDto.accountId)
                database.profileDao().upsertRelatedUsers(
                    relatedUsers.map { it.toEntity(profileDto.accountId) }
                )
                database.profileDao().upsertAccounts(
                    accounts.map { it.toEntity(profileDto.accountId) }
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
            database.teamDao().deleteAllRelationships()
            database.clubDao().deleteAllRelationships()
            database.profileDao().deleteAllRelatedUsers()
            database.profileDao().deleteAllAccounts()
            database.profileDao().deleteAllProfiles()
            database.teamDao().deleteAll()
            database.clubDao().deleteAll()
            database.teamHubCacheDao().deleteAll()
        }
    }

    private fun String.needsRepair(accountId: String): Boolean =
        this == accountId || looksLikeEmail()

    private fun String.looksLikeEmail(): Boolean =
        contains('@')

    private companion object {
        const val LINK_CANDIDATE_SCOPE = "link-candidate"
    }
}
