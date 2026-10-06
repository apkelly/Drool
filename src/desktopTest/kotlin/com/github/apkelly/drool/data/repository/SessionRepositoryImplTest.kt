package com.github.apkelly.drool.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import com.github.apkelly.drool.data.local.entity.CacheMetadataEntity
import com.github.apkelly.drool.data.local.entity.AccountEntity
import com.github.apkelly.drool.data.local.entity.ClubEntity
import com.github.apkelly.drool.data.local.entity.ClubRelationshipEntity
import com.github.apkelly.drool.data.local.entity.FixtureEntity
import com.github.apkelly.drool.data.local.entity.ProfileEntity
import com.github.apkelly.drool.data.local.entity.RelatedUserEntity
import com.github.apkelly.drool.data.local.entity.TeamEntity
import com.github.apkelly.drool.data.local.entity.TeamRelationshipEntity
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.Account
import com.github.apkelly.drool.domain.model.EmergencyContact
import com.github.apkelly.drool.domain.model.RelatedUser
import com.github.apkelly.drool.data.remote.DriblHttpException
import com.github.apkelly.drool.data.remote.DriblResponseException
import com.github.apkelly.drool.domain.model.AuthCredentials
import com.github.apkelly.drool.domain.model.AuthenticationException
import com.github.apkelly.drool.domain.model.AuthenticationFailure
import com.github.apkelly.drool.domain.model.ProfileApiEndpoint
import com.github.apkelly.drool.domain.model.LinkMemberException
import com.github.apkelly.drool.domain.model.LinkMemberFailure
import com.github.apkelly.drool.domain.model.TeamRelationship
import com.github.apkelly.drool.domain.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class SessionRepositoryImplTest {
    @Test
    fun restoreRequiresStoredToken() = runTest {
        fixture { repository, _, _, _, _ ->
            assertNull(repository.restoreSession())
            assertNull(repository.observeProfile().first())
        }

    }

    @Test
    fun restoreHydratesMissingProfileWithStoredToken() = runTest {
        fixture { repository, _, sports, store, database ->
            store.saveBearerToken("saved")
            sports.profile = Profile(
                accountId = "account",
                displayName = "Taylor",
                email = null,
                playingTeamIds = setOf("team-a"),
            )
            sports.accounts = listOf(
                Account("login", "andrew@example.com", "email", null)
            )
            sports.linkedUsers = listOf(
                relatedUser(
                    id = "person",
                    firstName = "Andrew",
                    lastName = "Kelly",
                    email = null,
                    avatarUrl = "andrew.png",
                    isGuardian = false,
                )
            )

            val session = repository.restoreSession()

            assertEquals("saved", session?.bearerToken)
            assertEquals("Andrew", session?.profile?.displayName)
            assertEquals("andrew@example.com", session?.profile?.email)
            assertEquals("account", store.getActiveAccountId())
            assertEquals("Andrew", repository.observeProfile().first()?.displayName)
            assertEquals(
                TeamRelationship.PlaysFor.name,
                database.teamDao().observeRelationships("account").first().single().relationship,
            )
        }
    }

    @Test
    fun restoreBuildsProfileFromJwtWithoutNetwork() = runTest {
        fixture { repository, _, sports, store, _ ->
            store.saveBearerToken(
                "header.eyJzdWIiOiJhY2NvdW50IiwibmFtZSI6IkFsZXgiLCJlbWFpbCI6ImFsZXhAZXhhbXBsZS5jb20ifQ.signature"
            )
            sports.failure = IllegalStateException("network should not be called")

            val session = repository.restoreSession()

            assertEquals("account", session?.profile?.accountId)
            assertEquals("Alex", session?.profile?.displayName)
            assertEquals("account", store.getActiveAccountId())
        }
    }

    @Test
    fun restoreUsesCachedProfileWithoutNetwork() = runTest {
        fixture { repository, _, sports, store, database ->
            store.saveBearerToken("saved")
            database.profileDao().upsert(ProfileEntity("account", "Taylor", null, true))
            database.profileDao().upsertRelatedUsers(
                listOf(
                    RelatedUserEntity(
                        "account",
                        "related",
                        "Sam Player",
                        "sam@example.com",
                        null,
                    )
                )
            )
            database.profileDao().upsertAccounts(
                listOf(AccountEntity("account", "club", "Example Club", "Manager", null))
            )
            sports.failure = IllegalStateException("network should not be called")

            val restored = repository.restoreSession()?.profile
            assertEquals("Taylor", restored?.displayName)
            assertEquals("Sam Player", restored?.relatedUsers?.single()?.displayName)
            assertEquals("Example Club", restored?.accounts?.single()?.name)
        }
    }

    @Test
    fun restoreRepairsCachedAccountIdDisplayNameFromJwt() = runTest {
        fixture { repository, _, sports, store, database ->
            store.saveBearerToken(
                "header.eyJzdWIiOiI2NDAzMTE4IiwidXNlcm5hbWUiOiJhbmR5IiwiZW1haWwiOiJhbmR5QGV4YW1wbGUuY29tIn0.signature"
            )
            database.profileDao().upsert(ProfileEntity("6403118", "6403118", null, true))
            sports.failure = IllegalStateException("network should not be called")

            val session = repository.restoreSession()

            assertEquals("andy", session?.profile?.displayName)
            assertEquals("andy", repository.observeProfile().first()?.displayName)
            assertEquals("andy@example.com", sports.relatedUsersEmail)

            database.profileDao().upsert(
                ProfileEntity(
                    "6403118",
                    "6403118",
                    "cached@example.com",
                    true,
                )
            )
            store.saveBearerToken(
                "header.eyJzdWIiOiI2NDAzMTE4IiwidXNlcm5hbWUiOiJhbmR5In0.signature"
            )
            repository.restoreSession()
            assertEquals("cached@example.com", sports.relatedUsersEmail)
        }
    }

    @Test
    fun restoreRepairsCachedEmailDisplayNameFromJwtFirstName() = runTest {
        fixture { repository, _, sports, store, database ->
            store.saveBearerToken(
                "header.eyJzdWIiOiJhY2NvdW50IiwibmFtZSI6ImFsZXhAZXhhbXBsZS5jb20iLCJmaXJzdF9uYW1lIjoiQWxleCIsImxhc3RfbmFtZSI6IlBsYXllciIsImVtYWlsIjoiYWxleEBleGFtcGxlLmNvbSJ9.signature"
            )
            database.profileDao().upsert(
                ProfileEntity(
                    accountId = "account",
                    displayName = "alex@example.com",
                    email = null,
                    active = true,
                )
            )
            sports.failure = IllegalStateException("association refresh may fail")

            val profile = repository.restoreSession()?.profile

            assertEquals("Alex", profile?.displayName)
            assertEquals("alex@example.com", profile?.email)
            assertEquals("Alex", repository.observeProfile().first()?.displayName)
        }
    }

    @Test
    fun restoreKeepsCachedNameWhenJwtCannotImproveIt() = runTest {
        fixture { repository, _, _, store, database ->
            database.profileDao().upsert(ProfileEntity("6403118", "Taylor", null, true))
            store.saveBearerToken(
                "header.eyJzdWIiOiI2NDAzMTE4IiwidXNlcm5hbWUiOiJhbmR5In0.signature"
            )
            assertEquals("Taylor", repository.restoreSession()?.profile?.displayName)

            database.profileDao().upsert(ProfileEntity("6403118", "6403118", null, true))
            store.saveBearerToken(
                "header.eyJzdWIiOiJvdGhlciIsInVzZXJuYW1lIjoic29tZW9uZSJ9.signature"
            )
            assertEquals("6403118", repository.restoreSession()?.profile?.displayName)

            store.saveBearerToken("header.eyJzdWIiOiI2NDAzMTE4In0.signature")
            assertEquals("6403118", repository.restoreSession()?.profile?.displayName)
        }
    }

    @Test
    fun restoreClearsRejectedTokenAndPropagatesServerFailure() = runTest {
        fixture { repository, _, sports, store, _ ->
            listOf(401, 403).forEach { status ->
                store.saveBearerToken("rejected")
                sports.failure = DriblHttpException(status, "url", "body")

                assertNull(repository.restoreSession())
                assertNull(store.getBearerToken())
            }

            store.saveBearerToken("saved")
            sports.failure = DriblHttpException(500, "url", "body")
            assertFailsWith<DriblHttpException> {
                repository.restoreSession()
            }
        }
    }

    @Test
    fun restoreClearsRejectedTokenWhenCachedProfileExists() = runTest {
        fixture { repository, _, sports, store, database ->
            database.profileDao().upsert(ProfileEntity("account", "Taylor", null, true))

            listOf(401, 403).forEach { status ->
                store.saveBearerToken("rejected")
                sports.failure = DriblHttpException(status, "url", "body")

                assertNull(repository.restoreSession())
                assertNull(store.getBearerToken())
            }

            store.saveBearerToken("saved")
            sports.failure = DriblHttpException(500, "url", "body")

            assertEquals("Taylor", repository.restoreSession()?.profile?.displayName)
            assertEquals("saved", store.getBearerToken())
        }
    }

    @Test
    fun signInPersistsProfileTokenAndPlayingTeams() = runTest {
        fixture { repository, auth, sports, store, database ->
            auth.result = com.github.apkelly.drool.domain.model.AuthenticationResult(
                bearerToken = "token",
                identity = com.github.apkelly.drool.domain.model.AuthenticationIdentity(
                    id = "account",
                    firstName = "API Taylor",
                    lastName = "Player",
                    email = "taylor@example.com",
                    avatarUrl = "https://example.test/taylor.png",
                ),
                relatedUsers = listOf(
                    relatedUser(
                        id = "related",
                        firstName = "Sam",
                        lastName = "Player",
                        email = "sam@example.com",
                        avatarUrl = "https://example.test/sam.png",
                    )
                ),
                accounts = listOf(
                    Account(
                        id = "club",
                        name = "Example Club",
                        subtitle = "Manager",
                        logoUrl = "https://example.test/club.png",
                    )
                ),
            )
            sports.relatedUsers = auth.result.relatedUsers
            sports.accounts = auth.result.accounts
            sports.profile = Profile(
                accountId = "account",
                displayName = "Taylor",
                email = "taylor@example.com",
                playingTeamIds = setOf("team-a"),
            )
            val credentials = AuthCredentials("base", "/signin", "user", "password")
            val session = repository.signIn(credentials)

            assertEquals("token", session.bearerToken)
            assertEquals("API Taylor", session.profile?.displayName)
            assertEquals("https://example.test/taylor.png", session.profile?.avatarUrl)
            assertEquals("Sam Player", session.profile?.relatedUsers?.single()?.displayName)
            assertEquals("Example Club", session.profile?.accounts?.single()?.name)
            assertEquals("user", auth.request?.username)
            assertEquals("token", store.getBearerToken())
            assertEquals("account", store.getActiveAccountId())
            assertEquals(
                TeamRelationship.PlaysFor.name,
                database.teamDao().observeRelationships("account").first().single().relationship,
            )
            val observed = repository.observeProfile().first()
            assertEquals("sam@example.com", observed?.relatedUsers?.single()?.email)
            assertEquals("Manager", observed?.accounts?.single()?.subtitle)
            assertEquals("user", sports.relatedUsersEmail)
            assertEquals(
                "Sam Player",
                repository.restoreSession()?.profile?.relatedUsers?.single()?.displayName,
            )
            assertEquals(2, sports.relatedUsersCalls)
            assertEquals(2, sports.accountsCalls)
        }
    }

    @Test
    fun signInBuildsProfileFromJwtWithoutProfileRequest() = runTest {
        fixture { repository, auth, sports, _, _ ->
            auth.result = com.github.apkelly.drool.domain.model.AuthenticationResult(
                bearerToken =
                    "header.eyJzdWIiOiI2NDAzMTE4In0.signature",
            )
            sports.failure = IllegalStateException("network should not be called")

            val session = repository.signIn(
                AuthCredentials("base", "/signin", " andy ", "password")
            )

            assertEquals("andy", session.profile?.displayName)
        }
    }

    @Test
    fun signInFallsBackToResolvedProfileForBlankApiIdentity() = runTest {
        fixture { repository, auth, sports, _, _ ->
            auth.result = com.github.apkelly.drool.domain.model.AuthenticationResult(
                bearerToken = "token",
                identity = com.github.apkelly.drool.domain.model.AuthenticationIdentity(
                    id = "account",
                    firstName = " ",
                    lastName = null,
                    email = null,
                    avatarUrl = null,
                ),
            )
            sports.profile = Profile(
                accountId = "account",
                displayName = "Taylor",
                email = "resolved@example.com",
                playingTeamIds = emptySet(),
                avatarUrl = "https://example.test/profile.png",
            )

            val profile = repository.signIn(
                AuthCredentials("base", "/signin", "user", "password")
            ).profile

            assertEquals("Taylor", profile?.displayName)
            assertEquals("resolved@example.com", profile?.email)
            assertEquals("https://example.test/profile.png", profile?.avatarUrl)
        }
    }

    @Test
    fun signInPersistsEmailUsernameWhenIdentityHasNoEmail() = runTest {
        fixture { repository, auth, sports, _, _ ->
            auth.result = com.github.apkelly.drool.domain.model.AuthenticationResult(
                bearerToken =
                    "header.eyJzdWIiOiJhY2NvdW50IiwibmFtZSI6IlRheWxvciJ9.signature",
            )
            sports.associationFailure = IOException("offline")

            val profile = repository.signIn(
                AuthCredentials("base", "/signin", " alex@example.com ", "password")
            ).profile

            assertEquals("Taylor", profile?.displayName)
            assertEquals("alex@example.com", profile?.email)
        }
    }

    @Test
    fun signInUsesRelatedUserFirstNameAndPhotoForMainProfile() = runTest {
        fixture { repository, auth, sports, _, _ ->
            auth.result = com.github.apkelly.drool.domain.model.AuthenticationResult(
                bearerToken =
                    "header.eyJzdWIiOiJhY2NvdW50IiwiZW1haWwiOiJhbGV4QGV4YW1wbGUuY29tIn0.signature",
            )
            sports.relatedUsers = listOf(
                relatedUser(
                    id = "person",
                    firstName = "Andrew",
                    lastName = "Player",
                    email = "alex@example.com",
                    avatarUrl = "https://example.test/andrew.png",
                    value = "alex@example.com",
                    source = "https://example.test/andrew.png",
                )
            )

            val profile = repository.signIn(
                AuthCredentials("base", "/signin", "alex@example.com", "password")
            ).profile

            assertEquals("Andrew Player", profile?.displayName)
            assertEquals("https://example.test/andrew.png", profile?.avatarUrl)
            assertEquals("Andrew Player", profile?.relatedUsers?.single()?.displayName)
        }
    }

    @Test
    fun signInPreservesEmbeddedAssociationsWhenDedicatedRefreshFails() = runTest {
        fixture { repository, auth, sports, _, _ ->
            auth.result = com.github.apkelly.drool.domain.model.AuthenticationResult(
                bearerToken = "token",
                relatedUsers = listOf(
                    relatedUser(
                        id = "related",
                        firstName = "Sam",
                        lastName = "Player",
                        email = null,
                        avatarUrl = null,
                    )
                ),
                accounts = listOf(
                    Account(
                        id = "club",
                        name = "Example Club",
                        subtitle = null,
                        logoUrl = null,
                    )
                ),
            )
            sports.profile = Profile(
                accountId = "account",
                displayName = "Taylor",
                email = null,
                playingTeamIds = emptySet(),
            )
            sports.associationFailure = IOException("offline")

            val profile = repository.signIn(
                AuthCredentials("base", "/signin", "user", "password")
            ).profile

            assertEquals("Sam Player", profile?.relatedUsers?.single()?.displayName)
            assertEquals("Example Club", profile?.accounts?.single()?.name)
            assertEquals(1, sports.relatedUsersCalls)
            assertEquals(1, sports.accountsCalls)
        }
    }

    @Test
    fun signInPropagatesAssociationRefreshCancellation() = runTest {
        fixture { repository, auth, sports, _, _ ->
            auth.result = com.github.apkelly.drool.domain.model.AuthenticationResult(
                bearerToken =
                    "header.eyJzdWIiOiJhY2NvdW50IiwibmFtZSI6IlRheWxvciJ9.signature",
            )
            sports.associationFailure = CancellationException("cancelled")

            assertFailsWith<CancellationException> {
                repository.signIn(
                    AuthCredentials("base", "/signin", "user", "password")
                )
            }
            assertEquals(0, sports.relatedUsersCalls)
            assertEquals(1, sports.accountsCalls)
        }
    }

    @Test
    fun profileApiDiagnosticsRunOneEndpointAtATimeAndRequireSessionScope() = runTest {
        fixture { repository, _, sports, store, database ->
            repository.refreshProfileEndpoint(
                ProfileApiEndpoint.RelatedUsers,
                "alex@example.com",
            )
            assertEquals(0, sports.relatedUsersCalls)

            store.saveBearerToken("token")
            repository.refreshProfileEndpoint(
                ProfileApiEndpoint.AccessAccounts,
                "",
            )
            assertEquals(0, sports.accountsCalls)

            store.setActiveAccountId("account")
            sports.relatedUsers = listOf(
                relatedUser(
                    id = "related-login",
                    firstName = "Sam",
                    lastName = "Player",
                    email = "sam@example.com",
                    avatarUrl = null,
                    dateOfBirth = "2012-03-04",
                )
            )
            sports.linkedUsers = listOf(
                relatedUser(
                    id = "child",
                    firstName = "Sam",
                    lastName = "Player",
                    email = null,
                    avatarUrl = "sam.png",
                    userId = "child",
                )
            )
            sports.accounts = listOf(
                Account(
                    id = "login",
                    name = "alex@example.com",
                    subtitle = "email",
                    logoUrl = null,
                )
            )

            repository.refreshProfileEndpoint(
                ProfileApiEndpoint.RelatedUsers,
                "alex@example.com",
            )
            assertEquals(1, sports.relatedUsersCalls)
            assertEquals(0, sports.accountsCalls)
            assertEquals("alex@example.com", sports.relatedUsersEmail)
            assertEquals(
                "Sam Player",
                database.profileDao().getRelatedUsers("account").single().displayName,
            )

            repository.refreshProfileEndpoint(ProfileApiEndpoint.AccessAccounts, "")
            assertEquals(1, sports.relatedUsersCalls)
            assertEquals(1, sports.accountsCalls)
            assertEquals(
                "alex@example.com",
                database.profileDao().getAccounts("account").single().name,
            )

            repository.refreshProfileEndpoint(ProfileApiEndpoint.LinkedUsers, "")
            assertEquals(1, sports.linkedUsersCalls)
            val mergedUser = database.profileDao().getRelatedUsers("account").single()
            assertEquals("child", mergedUser.id)
            assertEquals("Sam Player", mergedUser.displayName)
            assertEquals("sam@example.com", mergedUser.email)
            assertEquals("2012-03-04", mergedUser.dateOfBirth)
            assertEquals("sam.png", mergedUser.avatarUrl)
            assertEquals(
                true,
                mergedUser.isLinked,
            )

            repository.refreshProfileEndpoint(
                ProfileApiEndpoint.RelatedUsers,
                "alex@example.com",
            )
            val preservedUser = database.profileDao().getRelatedUsers("account").single()
            assertEquals("child", preservedUser.id)
            assertEquals("2012-03-04", preservedUser.dateOfBirth)
            assertEquals(true, preservedUser.isLinked)

            sports.failure = DriblHttpException(401, "url", "body")
            repository.refreshProfileEndpoint(
                ProfileApiEndpoint.RelatedUsers,
                "alex@example.com",
            )
            assertEquals(3, sports.relatedUsersCalls)
        }
    }

    @Test
    fun linkMemberFlowMapsCandidatesVerifiesTokenAndRefreshesLinkedUsers() = runTest {
            fixture { repository, _, sports, store, database ->
                store.saveBearerToken("token")
                store.setActiveAccountId("account")
                sports.linkCandidates = listOf(
                    relatedUser(
                        id = "candidate",
                        firstName = "Ethan",
                        lastName = "Kelly",
                        email = "ethan@example.com",
                        avatarUrl = "ethan.png",
                        dateOfBirth = "2012-03-04",
                        gender = "Male",
                        phoneNumber = "0400 000 000",
                        address = "1 Example Street",
                        emergencyContacts = listOf(
                            EmergencyContact("Andrew Kelly", "0400 111 111")
                        ),
                    )
                )
                sports.relatedUsers = sports.linkCandidates

                val candidate = repository.getLinkCandidates().single()
                assertEquals("Ethan Kelly", candidate.displayName)
                assertEquals("Male", candidate.gender)
                assertEquals("2012-03-04", candidate.dateOfBirth)
                assertEquals("0400 000 000", candidate.phoneNumber)
                assertEquals("1 Example Street", candidate.address)
                assertEquals(
                    listOf(EmergencyContact("Andrew Kelly", "0400 111 111")),
                    candidate.emergencyContacts,
                )

                repository.verifyLinkedUser("candidate", "123456")

                assertEquals("candidate" to "123456", sports.verifiedLink)
                assertEquals(
                    true,
                    database.profileDao().getRelatedUsers("account").single().isLinked,
                )
            }
        }

    @Test
    fun linkMemberRequiresSessionAndClassifiesFailures() = runTest {
            fixture { repository, _, sports, store, _ ->
                assertEquals(
                    LinkMemberFailure.SessionExpired,
                    assertFailsWith<LinkMemberException> {
                        repository.getLinkCandidates()
                    }.reason,
                )
                assertEquals(
                    LinkMemberFailure.SessionExpired,
                    assertFailsWith<LinkMemberException> {
                        repository.verifyLinkedUser("candidate", "123456")
                    }.reason,
                )

                store.saveBearerToken("token")
                assertEquals(
                    LinkMemberFailure.SessionExpired,
                    assertFailsWith<LinkMemberException> {
                        repository.verifyLinkedUser("candidate", "123456")
                    }.reason,
                )

                listOf(
                    DriblHttpException(401, "url", "body") to LinkMemberFailure.SessionExpired,
                    DriblHttpException(403, "url", "body") to LinkMemberFailure.SessionExpired,
                    DriblHttpException(422, "url", "body") to LinkMemberFailure.Unknown,
                    IOException("offline") to LinkMemberFailure.Offline,
                    IllegalStateException("unexpected") to LinkMemberFailure.Unknown,
                ).forEach { (failure, expected) ->
                    sports.failure = failure as Exception
                    assertEquals(
                        expected,
                        assertFailsWith<LinkMemberException> {
                            repository.getLinkCandidates()
                        }.reason,
                    )
                }

                sports.failure = DriblHttpException(422, "url", "body")
                assertEquals(
                    LinkMemberFailure.InvalidToken,
                    assertFailsWith<LinkMemberException> {
                        repository.verifyLinkedUser("candidate", "123456")
                    }.reason,
                )
                listOf(
                    399 to LinkMemberFailure.Unknown,
                    400 to LinkMemberFailure.InvalidToken,
                    499 to LinkMemberFailure.InvalidToken,
                    500 to LinkMemberFailure.Unknown,
                ).forEach { (status, expected) ->
                    sports.failure = DriblHttpException(status, "url", "body")
                    assertEquals(
                        expected,
                        assertFailsWith<LinkMemberException> {
                            repository.verifyLinkedUser("candidate", "123456")
                        }.reason,
                    )
                }

                sports.failure = LinkMemberException(LinkMemberFailure.InvalidToken)
                assertEquals(
                    LinkMemberFailure.InvalidToken,
                    assertFailsWith<LinkMemberException> {
                        repository.getLinkCandidates()
                    }.reason,
                )

                sports.failure = CancellationException("cancelled")
                assertFailsWith<CancellationException> {
                    repository.getLinkCandidates()
                }
            }
    }

    @Test
    fun relatedUserDiagnosticDoesNotRewriteAnotherActiveProfile() = runTest {
        fixture { repository, _, sports, store, database ->
            store.saveBearerToken("token")
            store.setActiveAccountId("account")
            database.profileDao().upsert(
                com.github.apkelly.drool.data.local.entity.ProfileEntity(
                    accountId = "other",
                    displayName = "Other",
                    email = "other@example.com",
                    active = true,
                )
            )
            sports.relatedUsers = listOf(
                relatedUser(
                    id = "person",
                    firstName = "Andrew",
                    lastName = null,
                    email = "alex@example.com",
                    avatarUrl = "https://example.test/andrew.png",
                )
            )

            repository.refreshProfileEndpoint(
                ProfileApiEndpoint.RelatedUsers,
                "alex@example.com",
            )

            assertEquals("Other", database.profileDao().getActive()?.displayName)
        }
    }

    @Test
    fun signInFailuresAreClassifiedAndCancellationPropagates() = runTest {
        fixture { repository, auth, _, _, _ ->
            val credentials = AuthCredentials("base", "/signin", "user", "password")
            listOf(
                DriblHttpException(401, "url", "body") to AuthenticationFailure.InvalidCredentials,
                DriblHttpException(403, "url", "body") to AuthenticationFailure.InvalidCredentials,
                DriblHttpException(500, "url", "body") to AuthenticationFailure.Unknown,
                IOException("offline") to AuthenticationFailure.Offline,
                DriblResponseException("invalid") to AuthenticationFailure.Unknown,
                SerializationException("invalid") to AuthenticationFailure.Unknown,
                IllegalStateException("unexpected") to AuthenticationFailure.Unknown,
            ).forEach { (failure, expected) ->
                auth.failure = failure
                val error = assertFailsWith<AuthenticationException> {
                    repository.signIn(credentials)
                }
                assertEquals(expected, error.reason)
            }

            auth.failure = CancellationException("cancelled")
            assertFailsWith<CancellationException> {
                repository.signIn(credentials)
            }
        }
    }

    @Test
    fun signOutClearsEveryCacheWhilePreservingTheme() = runTest {
        fixture { repository, _, _, store, database ->
            store.saveBearerToken("token")
            store.setActiveAccountId("account")
            store.setThemeMode(ThemeMode.Dark)
            database.profileDao().upsert(ProfileEntity("account", "Taylor", null, true))
            database.profileDao().upsertRelatedUsers(
                listOf(
                    RelatedUserEntity(
                        "account",
                        "related",
                        "Sam Player",
                        "sam@example.com",
                        null,
                    )
                )
            )
            database.profileDao().upsertAccounts(
                listOf(AccountEntity("account", "club", "Example Club", "Manager", null))
            )
            database.teamDao().upsertRelationship(
                TeamRelationshipEntity("account", "team", TeamRelationship.Following.name)
            )
            database.clubDao().upsertAll(
                listOf(ClubEntity("club", "Example Club", null, null, null, null))
            )
            database.clubDao().upsertRelationships(
                listOf(ClubRelationshipEntity("account", "club"))
            )
            database.teamDao().upsertAll(
                listOf(TeamEntity("team", "club", "Example Team", null, null, null, null, true))
            )
            database.fixtureDao().upsertAll(
                listOf(
                    FixtureEntity(
                        id = "fixture",
                        accountId = "account",
                        kickoffEpochMillis = 1,
                        homeTeamId = null,
                        homeTeamName = "Home",
                        awayTeamId = null,
                        awayTeamName = "Away",
                        competitionName = null,
                        venueName = null,
                        userTeamId = null,
                        status = "",
                    )
                )
            )
            database.cacheMetadataDao().upsert(CacheMetadataEntity("fixtures", "account", 1))

            repository.signOut()

            assertNull(store.getBearerToken())
            assertNull(store.getActiveAccountId())
            assertEquals(ThemeMode.Dark, store.observeThemeMode().first())
            assertNull(database.profileDao().getActive())
            assertEquals(0, database.clubDao().count())
            assertEquals(0, database.teamDao().count())
            assertEquals(0, database.fixtureDao().countForAccount("account"))
            assertEquals(emptyList(), database.teamDao().observeRelationships("account").first())
            assertEquals(
                emptyList(),
                database.clubDao().observeRelationships(listOf("account")).first(),
            )
            assertNull(database.cacheMetadataDao().get("fixtures", "account"))
            assertEquals(emptyList(), database.profileDao().getRelatedUsers("account"))
            assertEquals(emptyList(), database.profileDao().getAccounts("account"))
        }
    }

    @Test
    fun signOutWithoutAccountDeletesProfiles() = runTest {
        fixture { repository, _, _, _, database ->
            database.profileDao().upsert(ProfileEntity("account", "Taylor", null, true))
            repository.signOut()
            assertNull(database.profileDao().getActive())
        }
    }

    private fun relatedUser(
        id: String,
        firstName: String?,
        lastName: String?,
        email: String?,
        avatarUrl: String?,
        value: String? = null,
        source: String? = null,
        dateOfBirth: String? = null,
        userId: String? = null,
        gender: String? = null,
        phoneNumber: String? = null,
        address: String? = null,
        emergencyContacts: List<EmergencyContact> = emptyList(),
        isGuardian: Boolean? = null,
    ): RelatedUser {
        val displayName = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .takeIf(String::isNotBlank)
            ?: email
            ?: value
            ?: id
        return RelatedUser(
            id = userId ?: id,
            displayName = displayName,
            email = email ?: value?.takeIf { it.contains('@') },
            avatarUrl = avatarUrl ?: source,
            subjectUserId = userId ?: id,
            dateOfBirth = dateOfBirth,
            gender = gender,
            phoneNumber = phoneNumber,
            address = address,
            emergencyContacts = emergencyContacts,
            firstName = firstName,
            isGuardian = isGuardian,
        )
    }

    private suspend fun fixture(
        block: suspend (
            SessionRepositoryImpl,
            FakeAuthRemote,
            FakeSportsRemote,
            com.github.apkelly.drool.data.storage.BearerTokenStore,
            com.github.apkelly.drool.data.local.DroolDatabase,
        ) -> Unit,
    ) {
        TestDatabase().use { testDatabase ->
            TestTokenStore().use { testStore ->
                val auth = FakeAuthRemote()
                val sports = FakeSportsRemote()
                block(
                    SessionRepositoryImpl(
                        authApi = auth,
                        sportsApi = sports,
                        store = testStore.store,
                        database = testDatabase.database,
                    ),
                    auth,
                    sports,
                    testStore.store,
                    testDatabase.database,
                )
            }
        }
    }
}
