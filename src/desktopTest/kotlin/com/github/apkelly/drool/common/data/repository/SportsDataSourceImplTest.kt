package com.github.apkelly.drool.common.data.repository

import com.github.apkelly.dribl.api.DriblHttpException
import com.github.apkelly.dribl.api.DriblResponseException
import com.github.apkelly.drool.common.data.local.entity.ClubEntity
import com.github.apkelly.drool.common.data.local.entity.ClubRelationshipEntity
import com.github.apkelly.drool.common.data.local.entity.ProfileEntity
import com.github.apkelly.drool.common.data.local.entity.RelatedUserEntity
import com.github.apkelly.drool.common.data.local.entity.TeamEntity
import com.github.apkelly.drool.common.data.local.entity.TeamRelationshipEntity
import com.github.apkelly.drool.common.data.mapper.toDomain
import com.github.apkelly.drool.common.data.time.TimeProvider
import com.github.apkelly.drool.common.domain.model.CachedData
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.EmergencyContact
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.common.domain.model.OperationResult
import com.github.apkelly.drool.common.domain.model.RefereeAllocationDecision
import com.github.apkelly.drool.common.domain.model.RefreshFailure
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.Team
import com.github.apkelly.drool.common.domain.model.TeamAssociation
import com.github.apkelly.drool.common.domain.model.TeamHub
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
import com.github.apkelly.drool.common.domain.model.TeamSheetStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SportsDataSourceImplTest {
    @Test
    fun teamHubUsesProfileSessionAndSeparatesMatchesFromResults() = runTest {
        fixture { repository, remote, store, _, _ ->
            assertEquals(
                RefreshFailure.InvalidResponse,
                assertIs<OperationResult.Failed>(
                    repository.loadTeamHub("root", "team"),
                ).reason,
            )

            store.saveBearerToken("root-token")
            assertEquals(
                RefreshFailure.InvalidResponse,
                assertIs<OperationResult.Failed>(
                    repository.loadTeamHub("root", "team"),
                ).reason,
            )
            store.setActiveAccountId("root")
            remote.teamHub = teamHub(
                teamId = "team",
                fixtures = listOf(
                    fixture(
                        "upcoming",
                        200L,
                        "team",
                        "Home",
                        "away",
                        "Away",
                        null,
                        null,
                        "team",
                        "scheduled",
                    ),
                    fixture(
                        "result",
                        100L,
                        "team",
                        "Home",
                        "away",
                        "Away",
                        null,
                        null,
                        "team",
                        "complete",
                        homeScore = 2,
                        awayScore = 1,
                    ),
                ),
                ladderName = null,
                ladder = emptyList(),
            )

            val hub = repository.loadTeamHub("root", "team").teamHubData()
            val content = hub.value
            assertEquals(listOf("upcoming"), content.matches.map { it.id })
            assertEquals(listOf("result"), content.results.map { it.id })
            assertEquals("root-token" to "team", remote.teamHubRequest)

            repository.loadMatchDetails("child", "result")
            assertEquals("profile-token-child" to "result", remote.matchDetailsRequest)
            assertEquals(listOf("child"), remote.profileSessionUsers)
            val matchFixture = content.matches.single().copy(
                matchSheetId = "sheet",
                tenantId = "tenant",
                seasonId = "season",
            )
            assertIs<OperationResult.Success<*>>(
                repository.loadMatchSheet("child", matchFixture),
            )
            assertEquals("profile-token-child" to "upcoming", remote.matchSheetRequest)
            remote.matchSheet = remote.matchSheet.copy(
                members = listOf(
                    com.github.apkelly.drool.common.domain.model.MatchSheetMember(
                        id = "member",
                        userId = "user",
                        teamId = "home",
                        firstName = "Alex",
                        lastName = "Player",
                        imageUrl = null,
                        roleName = "Player",
                        jerseyNumber = "7",
                        available = true,
                    ),
                ),
            )
            assertIs<OperationResult.Success<*>>(
                repository.updateMemberAvailability("child", "member", false),
            )
            assertEquals(
                Triple("profile-token-child", "member", false),
                remote.memberAvailabilityRequest,
            )
            val status = assertIs<OperationResult.Success<TeamSheetStatus>>(
                repository.updateTeamSheetStatus(
                    "child",
                    "sheet",
                    "home",
                    TeamSheetAction.Confirm,
                ),
            )
            assertEquals(TeamSheetStatus.Confirmed, status.value)
            assertEquals(
                listOf("profile-token-child", "sheet", "home", "Confirm"),
                remote.teamSheetStatusRequest,
            )
            assertIs<OperationResult.Success<*>>(
                repository.loadMatchWeather(content.matches.single()),
            )
        }
    }

    @Test
    fun teamHubCachePersistsForTwentyFourHoursAndForceRefreshes() = runTest {
        fixture { repository, remote, store, clock, _ ->
            store.saveBearerToken("root-token")
            store.setActiveAccountId("root")
            remote.teamHub = teamHub(
                teamId = "team",
                fixtures = listOf(
                    fixture(
                        "match",
                        200L,
                        "team",
                        "Home",
                        "away",
                        "Away",
                        null,
                        null,
                        "team",
                        "scheduled",
                    ),
                ),
                ladderName = "Premier League",
                ladder = emptyList(),
            )

            val initial = repository.loadTeamHub("root", "team").teamHubData()
            assertFalse(initial.isStale)
            assertEquals(1, remote.teamHubCalls)

            clock.now = 24L * 60L * 60L * 1_000L - 1L
            remote.failure = IOException("offline")
            val freshCache = repository.loadTeamHub("root", "team").teamHubData()
            assertFalse(freshCache.isStale)
            assertEquals("Premier League", freshCache.value.ladderName)
            assertEquals(1, remote.teamHubCalls)

            clock.now += 1L
            val staleCache = repository.loadTeamHub("root", "team").teamHubData()
            assertTrue(staleCache.isStale)
            assertEquals(1, remote.teamHubCalls)

            assertEquals(
                RefreshFailure.Offline,
                assertIs<OperationResult.Failed>(
                    repository.loadTeamHub("root", "team", force = true),
                ).reason,
            )
            remote.failure = null
            repository.loadTeamHub("root", "team", force = true)
            assertEquals(2, remote.teamHubCalls)
        }
    }

    @Test
    fun teamHubCacheRespectsCompetitionContext() = runTest {
        fixture { repository, remote, store, _, _ ->
            store.saveBearerToken("root-token")
            store.setActiveAccountId("root")
            remote.teamHub = teamHub(
                teamId = "team",
                fixtures = emptyList(),
                ladderName = "Premier League",
                ladder = emptyList(),
            )

            repository.loadTeamHub(
                profileId = "root",
                teamId = "team",
                competitionName = "Premier League",
                leagueId = "premier",
            ).teamHubData()
            assertEquals("Premier League", remote.teamHubCompetitionName)
            assertEquals("premier", remote.teamHubLeagueId)
            assertEquals(1, remote.teamHubCalls)

            repository.loadTeamHub(
                profileId = "root",
                teamId = "team",
                competitionName = " premier league ",
                leagueId = "premier",
            ).teamHubData()
            assertEquals(1, remote.teamHubCalls)

            remote.failure = IOException("offline")
            assertEquals(
                RefreshFailure.Offline,
                assertIs<OperationResult.Failed>(
                    repository.loadTeamHub(
                        profileId = "root",
                        teamId = "team",
                        competitionName = "Championship",
                        leagueId = "championship",
                    ),
                ).reason,
            )
        }
    }

    @Test
    fun clubsRefreshAndObserveUseCacheFreshness() = runTest {
        fixture { repository, remote, store, clock, database ->
            store.saveBearerToken("token")
            remote.clubs = listOf(
                Club("club", "United", "U", null, "#008000", "#FFFFFF"),
            )

            val empty = repository.observeClubs().first()
            assertTrue(empty.value.isEmpty())
            assertEquals(null, empty.lastUpdatedEpochMillis)
            assertTrue(empty.isStale)
            assertEquals(RefreshResult.Updated, repository.refreshClubs(force = false))
            val fresh = repository.observeClubs().first()
            assertEquals("United", fresh.value.single().name)
            assertFalse(fresh.isStale)
            assertEquals(RefreshResult.NotModified, repository.refreshClubs(force = false))

            database.clubDao().deleteAll()
            assertEquals(RefreshResult.Updated, repository.refreshClubs(force = false))

            clock.now = 24L * 60L * 60L * 1_000L
            assertTrue(repository.observeClubs().first().isStale)
            assertEquals(RefreshResult.Updated, repository.refreshClubs(force = false))
        }
    }

    @Test
    fun teamsRefreshAndClubFilteringWork() = runTest {
        fixture { repository, remote, store, _, database ->
            store.saveBearerToken("token")
            database.teamDao().upsertAll(
                listOf(
                    TeamEntity(
                        "profile",
                        "club-profile",
                        "Profile Team",
                        null,
                        null,
                        null,
                        null,
                        true,
                    ),
                    TeamEntity(
                        "profile-same",
                        "club-a",
                        "Profile Team In Club",
                        null,
                        null,
                        null,
                        null,
                        true,
                    ),
                    TeamEntity("old", "club-a", "Old Team", null, null, null, null, true),
                ),
            )
            database.teamDao().upsertRelationship(
                TeamRelationshipEntity("account", "profile-same", TeamRelationship.Player.name),
            )
            remote.teams = listOf(
                Team("a", "club-a", "Alpha", null, null, null, null, true),
            )

            assertEquals(RefreshResult.Updated, repository.refreshTeams(clubId = "club-a"))
            assertEquals("club-a", remote.teamClubId)
            assertEquals(
                setOf("Alpha", "Profile Team In Club"),
                repository.observeTeams("club-a").first().value.map { it.name }.toSet(),
            )
            assertEquals(
                "Profile Team",
                repository.observeTeams("club-profile").first().value.single().name,
            )
            assertEquals(
                RefreshResult.NotModified,
                repository.refreshTeams(force = false, clubId = "club-a"),
            )

            remote.teams = listOf(
                Team("global", "club-b", "Global Team", null, null, null, null, true),
            )
            assertEquals(RefreshResult.Updated, repository.refreshTeams(force = true))
            assertEquals(null, remote.teamClubId)
            assertEquals(
                setOf("Alpha", "Profile Team", "Profile Team In Club", "Global Team"),
                repository.observeTeams(null).first().value.map { it.name }.toSet(),
            )
        }
    }

    @Test
    fun fixturesRefreshAndObserveForActiveAccount() = runTest {
        fixture { repository, remote, store, _, database ->
            store.saveBearerToken("token")
            store.setActiveAccountId("account")
            database.profileDao().upsert(ProfileEntity("account", "Alex", null, true))
            remote.fixtures = listOf(
                fixture(
                    id = "fixture",
                    kickoffEpochMillis = 123,
                    homeTeamId = "home",
                    homeTeamName = "Home",
                    awayTeamId = "away",
                    awayTeamName = "Away",
                    competitionName = "League",
                    venueName = "Oval",
                    userTeamId = "home",
                    status = "scheduled",
                ),
            )

            val empty = repository.observeFixtures().first()
            assertTrue(empty.value.isEmpty())
            assertEquals(null, empty.lastUpdatedEpochMillis)
            assertEquals(RefreshResult.Updated, repository.refreshFixtures(force = false))
            val cached = repository.observeFixtures().first()
            assertEquals("fixture", cached.value.single().id)
            assertFalse(cached.isStale)
            assertEquals(RefreshResult.NotModified, repository.refreshFixtures(force = false))

            remote.failure = IOException("offline")
            val failed = assertIs<RefreshResult.Failed>(
                repository.refreshFixtures(force = true),
            )
            assertTrue(failed.hasCachedData)
        }
    }

    @Test
    fun upcomingScheduleRetainsCurrentFixturesAndExcludesFinishedOnes() {
        val nowEpochMillis = 20_000_000L
        val finished = fixture(
            id = "finished",
            kickoffEpochMillis = nowEpochMillis - 1,
            homeTeamId = "home",
            homeTeamName = "Home",
            awayTeamId = "away",
            awayTeamName = "Away",
            competitionName = null,
            venueName = null,
            userTeamId = null,
            status = "complete",
        )
        val live = finished.copy(
            id = "live",
            kickoffEpochMillis = 0,
            status = FixtureStatus.Live,
        )
        val current = finished.copy(
            id = "current",
            kickoffEpochMillis = nowEpochMillis - 2L * 60L * 60L * 1_000L,
            status = FixtureStatus.Pending,
        )
        val expired = finished.copy(
            id = "expired",
            kickoffEpochMillis = nowEpochMillis - 3L * 60L * 60L * 1_000L,
            status = FixtureStatus.Pending,
        )
        val now = finished.copy(
            id = "now",
            kickoffEpochMillis = nowEpochMillis,
            status = FixtureStatus.Scheduled,
        )
        val future = finished.copy(
            id = "future",
            kickoffEpochMillis = nowEpochMillis + 1,
            status = FixtureStatus.Scheduled,
        )

        assertEquals(
            listOf("live", "current", "now", "future"),
            listOf(finished, live, current, expired, now, future)
                .upcomingAt(nowEpochMillis)
                .map(Fixture::id),
        )
    }

    @Test
    fun scheduleIncludesTwoWeeksOfHistory() {
        val nowEpochMillis = 20L * 24L * 60L * 60L * 1_000L
        val recent = fixture(
            id = "recent",
            kickoffEpochMillis = nowEpochMillis - 7L * 24L * 60L * 60L * 1_000L,
            homeTeamId = "home",
            homeTeamName = "Home",
            awayTeamId = "away",
            awayTeamName = "Away",
            competitionName = null,
            venueName = null,
            userTeamId = null,
            status = "complete",
        )
        val old = recent.copy(
            id = "old",
            kickoffEpochMillis = nowEpochMillis - 15L * 24L * 60L * 60L * 1_000L,
        )
        val future = recent.copy(
            id = "future",
            kickoffEpochMillis = nowEpochMillis + 1,
            status = FixtureStatus.Scheduled,
        )
        val oldLive = old.copy(id = "old-live", status = FixtureStatus.Live)

        assertEquals(
            listOf("recent", "future", "old-live"),
            listOf(old, recent, future, oldLive)
                .recentAndUpcomingAt(nowEpochMillis)
                .map(Fixture::id),
        )
    }

    @Test
    fun linkedProfilesRefreshAndObserveAttributedFixturesAndTeams() = runTest {
        fixture { repository, remote, store, clock, database ->
            store.saveBearerToken("root-token")
            remote.fixtures = listOf(
                fixture(
                    id = "shared-fixture",
                    kickoffEpochMillis = 123,
                    homeTeamId = "team",
                    homeTeamName = "Home",
                    awayTeamId = null,
                    awayTeamName = "Away",
                    competitionName = null,
                    venueName = null,
                    userTeamId = "team",
                    status = "scheduled",
                ),
            )
            remote.profileTeams = listOf(
                association(
                    team = Team(
                        "team",
                        "club",
                        "Under 12",
                        null,
                        null,
                        null,
                        null,
                        true,
                    ),
                    relationship = TeamRelationship.Following.name,
                ),
            )
            remote.profileClubs = listOf(
                Club("club", "United", "U", null, "#008000", "#FFFFFF"),
            )
            remote.refereeClubsByToken = mapOf(
                "profile-token-child" to listOf(
                    com.github.apkelly.drool.common.domain.model.RefereeClub(
                        id = "referee-club",
                        name = "Referee Club",
                        logoUrl = null,
                        timeZone = "Australia/Sydney",
                    ),
                ),
            )
            remote.refereeFixtures = listOf(
                Fixture(
                    id = "referee-match",
                    kickoffEpochMillis = 2_000L,
                    homeTeamId = "home",
                    homeTeamName = "Home",
                    awayTeamId = "away",
                    awayTeamName = "Away",
                    competitionName = "Competition",
                    venueName = "Field",
                    userTeamId = null,
                    status = FixtureStatus.Scheduled,
                    role = FixtureRole.Referee,
                    refereeAllocationId = "allocation",
                    refereeStatus = "accepted",
                ),
            )
            val profiles = listOf(
                LinkedProfile(
                    id = "root",
                    displayName = "Alex",
                    avatarUrl = null,
                    isPrimary = true,
                ),
                LinkedProfile("child", "Jude", null, isPrimary = false),
            )
            database.profileDao().upsertRelatedUsers(
                listOf(
                    RelatedUserEntity(
                        ownerAccountId = "root",
                        id = "child",
                        displayName = "Jude",
                        email = null,
                        avatarUrl = null,
                        isLinked = true,
                    ),
                ),
            )
            remote.personalInformationDto = relatedUser(
                id = "child",
                firstName = "Jude",
                lastName = "Bellingham",
                email = "jude.bellingham@example.com",
                avatarUrl = "jude.png",
                dateOfBirth = "2012-03-04",
                gender = "male",
                phoneNumber = "0400 000 000",
                address = "1 Example Street",
                emergencyContacts = listOf(
                    EmergencyContact("Jordan Pickford", "0400 333 333"),
                ),
            )
            remote.emergencyContacts = listOf(
                EmergencyContact("Harry Kane", "0400 111 111"),
            )

            assertEquals(
                mapOf(
                    "root" to RefreshResult.Updated,
                    "child" to RefreshResult.Updated,
                ),
                repository.refreshLinkedProfiles(profiles, force = false),
            )
            assertEquals(listOf("child"), remote.profileSessionUsers)
            assertEquals(listOf<String?>("root", "child"), remote.fixtureUserIds)
            assertEquals(listOf("profile-token-child"), remote.refereeFixtureTokens)
            assertEquals(listOf("child"), remote.refereeFixtureUserIds)

            val fixtures =
                repository.observeLinkedProfileFixtures(setOf("root", "child")).first()
            assertEquals(3, fixtures.size)
            assertEquals(setOf("root", "child"), fixtures.mapNotNull { it.profileId }.toSet())
            assertEquals(
                "child",
                fixtures.single { it.role == FixtureRole.Referee }.profileId,
            )

            val teams = repository.observeProfileTeams(setOf("root", "child")).first()
            assertEquals(2, teams.size)
            assertTrue(teams.all { it.relationship == TeamRelationship.Following })
            val clubs = repository.observeProfileClubs(setOf("root", "child")).first()
            assertEquals(2, clubs.size)
            assertEquals(setOf("root", "child"), clubs.map { it.profileId }.toSet())
            val personal = database.profileDao().getRelatedUser("root", "child")
            assertEquals("Jude Bellingham", personal?.displayName)
            assertEquals("male", personal?.gender)
            assertEquals("2012-03-04", personal?.dateOfBirth)
            assertEquals("0400 000 000", personal?.phoneNumber)
            assertEquals("1 Example Street", personal?.address)
            assertEquals(
                listOf(
                    EmergencyContact("Jordan Pickford", "0400 333 333"),
                    EmergencyContact("Harry Kane", "0400 111 111"),
                ),
                personal?.toDomain()?.emergencyContacts,
            )

            database.teamDao().upsertRelationship(
                TeamRelationshipEntity("child", "team", "PlaysFor"),
            )
            assertEquals(
                TeamRelationship.None,
                repository.observeProfileTeams(setOf("child")).first().single().relationship,
            )
            database.teamDao().upsertRelationship(
                TeamRelationshipEntity("child", "missing", "Following"),
            )
            assertEquals(
                1,
                repository.observeProfileTeams(setOf("child")).first().size,
            )
            database.clubDao().upsertRelationships(
                listOf(ClubRelationshipEntity("child", "missing")),
            )
            assertEquals(
                1,
                repository.observeProfileClubs(setOf("child")).first().size,
            )
            assertEquals(
                mapOf(
                    "root" to RefreshResult.NotModified,
                    "child" to RefreshResult.NotModified,
                ),
                repository.refreshLinkedProfiles(profiles, force = false),
            )
            remote.personalInformationDto = remote.personalInformationDto?.copy(
                phoneNumber = "0400 999 999",
                emergencyContacts = emptyList(),
            )
            remote.emergencyContacts = listOf(
                EmergencyContact("Bukayo Saka", "0400 444 444"),
            )
            assertEquals(
                RefreshResult.NotModified,
                repository.refreshLinkedProfiles(profiles, force = false)["child"],
            )
            val hydratedFromCachedRefresh =
                database.profileDao().getRelatedUser("root", "child")
            assertEquals("0400 999 999", hydratedFromCachedRefresh?.phoneNumber)
            assertEquals(
                listOf(EmergencyContact("Bukayo Saka", "0400 444 444")),
                hydratedFromCachedRefresh?.toDomain()?.emergencyContacts,
            )
            clock.now = 7L * 60L * 60L * 1_000L
            assertTrue(
                repository.refreshLinkedProfiles(profiles, force = false)
                    .values
                    .all { it == RefreshResult.Updated },
            )
            assertTrue(
                repository.refreshLinkedProfiles(profiles, force = true)
                    .values
                    .all { it == RefreshResult.Updated },
            )
            remote.personalInformationDto = relatedUser(
                id = "child",
                firstName = null,
                lastName = null,
                email = null,
                avatarUrl = null,
            )
            remote.emergencyContacts = emptyList()
            assertEquals(
                listOf(EmergencyContact("Bukayo Saka", "0400 444 444")),
                database.profileDao()
                    .getRelatedUser("root", "child")
                    ?.toDomain()
                    ?.emergencyContacts,
            )
            repository.refreshLinkedProfiles(profiles, force = true)
            val preserved = database.profileDao().getRelatedUser("root", "child")
            assertEquals("Jude Bellingham", preserved?.displayName)
            assertEquals("jude.bellingham@example.com", preserved?.email)
            assertEquals("jude.png", preserved?.avatarUrl)
            assertEquals("male", preserved?.gender)
            assertEquals(
                listOf(EmergencyContact("Bukayo Saka", "0400 444 444")),
                preserved?.toDomain()?.emergencyContacts,
            )
            database.profileDao().upsertRelatedUsers(
                listOf(checkNotNull(preserved).copy(emergencyContactsJson = null)),
            )
            repository.refreshLinkedProfiles(profiles, force = true)
            assertEquals(
                null,
                database.profileDao()
                    .getRelatedUser("root", "child")
                    ?.emergencyContactsJson,
            )
            assertEquals(
                RefreshResult.Updated,
                repository.refreshLinkedProfiles(
                    listOf(
                        LinkedProfile("root", "Alex", null, isPrimary = true),
                        LinkedProfile("orphan", "Orphan", null, isPrimary = false),
                    ),
                    force = true,
                )["orphan"],
            )
        }
    }

    @Test
    fun linkedProfileRefreshReportsAuthenticationAndProfileFailuresIndependently() = runTest {
        fixture { repository, remote, store, _, _ ->
            val profiles = listOf(
                LinkedProfile("root", "Alex", null, isPrimary = true),
                LinkedProfile("child", "Jude", null, isPrimary = false),
            )
            val unauthorized = repository.refreshLinkedProfiles(profiles)
            assertTrue(
                unauthorized.values.all {
                    it == RefreshResult.Failed(RefreshFailure.Unauthorized, false)
                },
            )

            store.saveBearerToken("root-token")
            assertEquals(
                emptyMap(),
                repository.refreshLinkedProfiles(emptyList()),
            )
            assertTrue(
                repository.refreshLinkedProfiles(
                    listOf(
                        LinkedProfile("child", "Jude", null, isPrimary = false),
                        LinkedProfile("root", "Alex", null, isPrimary = true),
                    ),
                    force = true,
                ).values.all { it == RefreshResult.Updated },
            )
            val invalidProfiles = repository.refreshLinkedProfiles(
                listOf(LinkedProfile("child", "Jude", null, isPrimary = false)),
            )
            assertEquals(
                RefreshResult.Failed(RefreshFailure.InvalidResponse, false),
                invalidProfiles["child"],
            )
            remote.profileSessionFailureUsers += "child"
            val results = repository.refreshLinkedProfiles(profiles, force = true)
            assertEquals(RefreshResult.Updated, results["root"])
            assertEquals(
                RefreshResult.Failed(RefreshFailure.Unknown, false),
                results["child"],
            )
            assertEquals(
                RefreshResult.Failed(RefreshFailure.Unknown, false),
                repository.refreshLinkedProfiles(profiles, force = false)["child"],
            )
        }
    }

    @Test
    fun refreshRequiresAuthenticationAndPreservesCachedFlag() = runTest {
        fixture { repository, _, _, _, database ->
            val empty = assertIs<RefreshResult.Failed>(repository.refreshClubs())
            assertEquals(RefreshFailure.Unauthorized, empty.reason)
            assertFalse(empty.hasCachedData)

            database.clubDao().upsertAll(
                listOf(ClubEntity("club", "United", null, null, null, null)),
            )
            val cached = assertIs<RefreshResult.Failed>(repository.refreshClubs())
            assertTrue(cached.hasCachedData)

            val noTokenFixtures = assertIs<RefreshResult.Failed>(repository.refreshFixtures())
            assertEquals(RefreshFailure.Unauthorized, noTokenFixtures.reason)
        }
    }

    @Test
    fun fixtureRefreshRequiresAccountContext() = runTest {
        fixture { repository, _, store, _, _ ->
            store.saveBearerToken("token")
            val result = assertIs<RefreshResult.Failed>(repository.refreshFixtures())
            assertEquals(RefreshFailure.Unauthorized, result.reason)
        }
    }

    @Test
    fun httpFailuresAreClassified() = runTest {
        fixture { repository, remote, store, _, _ ->
            store.saveBearerToken("token")
            mapOf(
                401 to RefreshFailure.Unauthorized,
                403 to RefreshFailure.Forbidden,
                500 to RefreshFailure.Server,
                600 to RefreshFailure.InvalidResponse,
                422 to RefreshFailure.InvalidResponse,
            ).forEach { (status, expected) ->
                remote.failure = DriblHttpException(status, "url", "body")
                val result = assertIs<RefreshResult.Failed>(repository.refreshClubs(force = true))
                assertEquals(expected, result.reason)
            }

            remote.failure = DriblResponseException("bad contract")
            val response = assertIs<RefreshResult.Failed>(repository.refreshClubs(force = true))
            assertEquals(RefreshFailure.InvalidResponse, response.reason)

            remote.failure = SerializationException("bad json")
            val serialization = assertIs<RefreshResult.Failed>(repository.refreshClubs(force = true))
            assertEquals(RefreshFailure.InvalidResponse, serialization.reason)

            remote.failure = IOException("offline")
            val offline = assertIs<RefreshResult.Failed>(repository.refreshClubs(force = true))
            assertEquals(RefreshFailure.Offline, offline.reason)

            remote.failure = IllegalStateException("unexpected")
            val unknown = assertIs<RefreshResult.Failed>(repository.refreshClubs(force = true))
            assertEquals(RefreshFailure.Unknown, unknown.reason)

            remote.failure = CancellationException("cancelled")
            assertFailsWith<CancellationException> {
                repository.refreshClubs(force = true)
            }
        }
    }

    @Test
    fun followingRelationshipsAreAccountScoped() = runTest {
        fixture { repository, _, store, _, database ->
            repository.setFollowing("ignored", true)

            store.setActiveAccountId("account")
            database.profileDao().upsert(ProfileEntity("account", "Alex", null, true))
            repository.setFollowing("team", true)
            assertEquals(
                TeamRelationship.Following,
                repository.observeTeamRelationships().first()["team"],
            )

            database.teamDao().upsertRelationship(
                TeamRelationshipEntity("account", "unknown", "unexpected"),
            )
            assertEquals(
                TeamRelationship.None,
                repository.observeTeamRelationships().first()["unknown"],
            )

            repository.setFollowing("team", false)
            assertEquals(null, repository.observeTeamRelationships().first()["team"])
        }
    }

    @Test
    fun emptyAccountFlowsReturnEmptyCachedData() = runTest {
        fixture { repository, _, _, _, _ ->
            assertTrue(repository.observeFixtures().first().value.isEmpty())
            assertTrue(repository.observeTeamRelationships().first().isEmpty())
        }
    }

    @Test
    fun refereeAllocationDecisionsUseProfileSessionAndRecoveredStatuses() = runTest {
        fixture { repository, remote, store, _, _ ->
            assertEquals(
                RefreshResult.Failed(RefreshFailure.Unauthorized, hasCachedData = true),
                repository.updateRefereeAllocation(
                    LinkedProfile("root", "Alex", null, isPrimary = true),
                    "allocation-0",
                    RefereeAllocationDecision.Accept,
                ),
            )

            store.saveBearerToken("root-token")
            assertEquals(
                RefreshResult.Updated,
                repository.updateRefereeAllocation(
                    LinkedProfile("root", "Alex", null, isPrimary = true),
                    "allocation-0",
                    RefereeAllocationDecision.Accept,
                ),
            )
            assertEquals(
                Triple("root-token", "allocation-0", "accepted"),
                remote.updatedRefereeAllocation,
            )

            val profile = LinkedProfile("child", "Jude", null, isPrimary = false)

            assertEquals(
                RefreshResult.Updated,
                repository.updateRefereeAllocation(
                    profile,
                    "allocation-1",
                    RefereeAllocationDecision.Accept,
                ),
            )
            assertEquals(
                Triple("profile-token-child", "allocation-1", "accepted"),
                remote.updatedRefereeAllocation,
            )

            repository.updateRefereeAllocation(
                profile,
                "allocation-2",
                RefereeAllocationDecision.Decline,
            )
            assertEquals(
                Triple("profile-token-child", "allocation-2", "request_contact"),
                remote.updatedRefereeAllocation,
            )
        }
    }

    private fun fixture(
        id: String,
        kickoffEpochMillis: Long,
        homeTeamId: String?,
        homeTeamName: String,
        awayTeamId: String?,
        awayTeamName: String,
        competitionName: String?,
        venueName: String?,
        userTeamId: String?,
        status: String?,
        homeScore: Int? = null,
        awayScore: Int? = null,
    ): com.github.apkelly.drool.common.domain.model.Fixture = com.github.apkelly.drool.common.domain.model.Fixture(
        id = id,
        kickoffEpochMillis = kickoffEpochMillis,
        homeTeamId = homeTeamId,
        homeTeamName = homeTeamName,
        awayTeamId = awayTeamId,
        awayTeamName = awayTeamName,
        competitionName = competitionName,
        venueName = venueName,
        userTeamId = userTeamId,
        status = when (status) {
            "scheduled" -> com.github.apkelly.drool.common.domain.model.FixtureStatus.Scheduled
            "complete" -> com.github.apkelly.drool.common.domain.model.FixtureStatus.Completed
            else -> com.github.apkelly.drool.common.domain.model.FixtureStatus.Unknown
        },
        homeScore = homeScore,
        awayScore = awayScore,
    )

    private fun teamHub(
        teamId: String,
        fixtures: List<com.github.apkelly.drool.common.domain.model.Fixture>,
        ladderName: String?,
        ladder: List<com.github.apkelly.drool.common.domain.model.LadderEntry>,
    ): com.github.apkelly.drool.common.domain.model.TeamHub {
        val (results, matches) = fixtures.partition {
            it.status == com.github.apkelly.drool.common.domain.model.FixtureStatus.Completed
        }
        return com.github.apkelly.drool.common.domain.model.TeamHub(
            teamId = teamId,
            matches = matches,
            results = results,
            ladderName = ladderName,
            ladder = ladder,
        )
    }

    private fun association(
        team: com.github.apkelly.drool.common.domain.model.Team,
        relationship: String,
    ) = com.github.apkelly.drool.common.domain.model.TeamAssociation(
        team,
        com.github.apkelly.drool.common.domain.model.TeamRelationship.valueOf(relationship),
    )

    private fun relatedUser(
        id: String,
        firstName: String?,
        lastName: String?,
        email: String?,
        avatarUrl: String?,
        dateOfBirth: String? = null,
        gender: String? = null,
        phoneNumber: String? = null,
        address: String? = null,
        emergencyContacts: List<EmergencyContact> = emptyList(),
    ) = com.github.apkelly.drool.common.domain.model.RelatedUser(
        id = id,
        displayName = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .takeIf(String::isNotBlank)
            ?: email
            ?: id,
        email = email,
        avatarUrl = avatarUrl,
        subjectUserId = id,
        isLinked = true,
        dateOfBirth = dateOfBirth,
        gender = gender,
        phoneNumber = phoneNumber,
        address = address,
        emergencyContacts = emergencyContacts,
    )

    private suspend fun fixture(
        block: suspend (
            TestSportsRepositories,
            FakeSportsRemote,
            com.github.apkelly.drool.common.data.storage.BearerTokenStore,
            MutableTimeProvider,
            com.github.apkelly.drool.common.data.local.DroolDatabase,
        ) -> Unit,
    ) {
        TestDatabase().use { testDatabase ->
            TestTokenStore().use { testStore ->
                val remote = FakeSportsRemote()
                val clock = MutableTimeProvider()
                block(
                    TestSportsRepositories(
                        api = remote,
                        database = testDatabase.database,
                        store = testStore.store,
                        timeProvider = clock,
                    ),
                    remote,
                    testStore.store,
                    clock,
                    testDatabase.database,
                )
            }
        }
    }
}

private class MutableTimeProvider(
    var now: Long = 0,
) : TimeProvider {
    override fun nowEpochMillis(): Long = now
}

private fun OperationResult<CachedData<TeamHub>>.teamHubData(): CachedData<TeamHub> =
    assertIs<OperationResult.Success<CachedData<TeamHub>>>(this).value
