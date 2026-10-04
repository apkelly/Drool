package com.github.apkelly.drool.data.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlinx.coroutines.Dispatchers
import com.github.apkelly.drool.data.local.DroolDatabase
import com.github.apkelly.drool.data.remote.AuthRemoteDataSource
import com.github.apkelly.drool.data.remote.SportsRemoteDataSource
import com.github.apkelly.drool.domain.model.AuthenticationResult
import com.github.apkelly.drool.data.remote.model.AuthApiRequest
import com.github.apkelly.drool.data.storage.BearerTokenStore
import com.github.apkelly.drool.data.storage.createBearerTokenStore
import com.github.apkelly.drool.domain.model.Account
import com.github.apkelly.drool.domain.model.Club
import com.github.apkelly.drool.domain.model.EmergencyContact
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FixtureStatus
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.RelatedUser
import com.github.apkelly.drool.domain.model.Team
import com.github.apkelly.drool.domain.model.TeamAssociation
import com.github.apkelly.drool.domain.model.TeamHub

internal class TestDatabase : AutoCloseable {
    private val file = File.createTempFile("drool-test-", ".db").apply { delete() }
    val database: DroolDatabase =
        Room.databaseBuilder<DroolDatabase>(name = file.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    override fun close() {
        database.close()
        file.delete()
    }
}

internal class TestTokenStore : AutoCloseable {
    private val file = File.createTempFile("drool-preferences-", ".preferences_pb").apply { delete() }
    val store: BearerTokenStore = createBearerTokenStore { file.absolutePath }

    override fun close() {
        file.delete()
    }
}

internal class FakeAuthRemote : AuthRemoteDataSource {
    var result = AuthenticationResult("token")
    var request: AuthApiRequest? = null
    var failure: Throwable? = null

    override suspend fun signIn(request: AuthApiRequest): AuthenticationResult {
        failure?.let { throw it }
        this.request = request
        return result
    }
}

internal class FakeSportsRemote : SportsRemoteDataSource {
    var clubs: List<Club> = emptyList()
    var teams: List<Team> = emptyList()
    var fixtures: List<Fixture> = emptyList()
    var profile = Profile("account", "Alex", "alex@example.com")
    var relatedUsers: List<RelatedUser> = emptyList()
    var linkedUsers: List<RelatedUser>? = null
    var accounts: List<Account> = emptyList()
    var profileTeams: List<TeamAssociation> = emptyList()
    var profileClubs: List<Club> = emptyList()
    var personalInformationDto: RelatedUser? = null
    var emergencyContacts: List<EmergencyContact> = emptyList()
    var teamHub = TeamHub("team", emptyList(), emptyList(), null, emptyList())
    var matchDetails = Fixture(
        id = "match",
        kickoffEpochMillis = 0L,
        homeTeamId = null,
        homeTeamName = "Home",
        awayTeamId = null,
        awayTeamName = "Away",
        competitionName = null,
        venueName = null,
        userTeamId = null,
        status = FixtureStatus.Unknown,
    )
    var teamHubRequest: Pair<String, String>? = null
    var matchDetailsRequest: Pair<String, String>? = null
    var linkCandidates: List<RelatedUser> = emptyList()
    var verifiedLink: Pair<String, String>? = null
    val profileSessionUsers = mutableListOf<String>()
    val profileSessionFailureUsers = mutableSetOf<String>()
    val fixtureUserIds = mutableListOf<String?>()
    var teamClubId: String? = null
    var relatedUsersCalls = 0
    var relatedUsersEmail: String? = null
    var accountsCalls = 0
    var linkedUsersCalls = 0
    var failure: Exception? = null
    var associationFailure: Exception? = null

    override suspend fun fetchClubs(bearerToken: String): List<Club> {
        failure?.let { throw it }
        return clubs
    }

    override suspend fun fetchTeams(bearerToken: String, clubId: String?): List<Team> {
        failure?.let { throw it }
        teamClubId = clubId
        return teams
    }

    override suspend fun fetchFixtures(
        bearerToken: String,
        userId: String?,
    ): List<Fixture> {
        failure?.let { throw it }
        fixtureUserIds += userId
        return fixtures
    }

    override suspend fun fetchProfile(bearerToken: String): Profile {
        failure?.let { throw it }
        return profile
    }

    override suspend fun fetchRelatedUsers(
        bearerToken: String,
        email: String,
    ): List<RelatedUser> {
        relatedUsersCalls++
        relatedUsersEmail = email
        associationFailure?.let { throw it }
        failure?.let { throw it }
        return relatedUsers
    }

    override suspend fun fetchAccounts(bearerToken: String): List<Account> {
        accountsCalls++
        associationFailure?.let { throw it }
        failure?.let { throw it }
        return accounts
    }

    override suspend fun fetchLinkedUsers(
        bearerToken: String,
    ): List<RelatedUser> {
        linkedUsersCalls++
        associationFailure?.let { throw it }
        failure?.let { throw it }
        return linkedUsers ?: relatedUsers
    }

    override suspend fun fetchLinkCandidates(
        bearerToken: String,
    ): List<RelatedUser> {
        failure?.let { throw it }
        return linkCandidates
    }

    override suspend fun verifyLinkedUser(
        bearerToken: String,
        candidateId: String,
        token: String,
    ) {
        failure?.let { throw it }
        verifiedLink = candidateId to token
    }

    override suspend fun createProfileSession(bearerToken: String, userId: String): String {
        failure?.let { throw it }
        if (userId in profileSessionFailureUsers) {
            throw IllegalStateException("Profile session failed for $userId")
        }
        profileSessionUsers += userId
        return "profile-token-$userId"
    }

    override suspend fun fetchPersonalInformation(
        bearerToken: String,
        userId: String,
    ): RelatedUser {
        failure?.let { throw it }
        return personalInformationDto ?: RelatedUser(
            id = userId,
            displayName = userId,
            email = null,
            avatarUrl = null,
        )
    }

    override suspend fun fetchEmergencyContacts(
        bearerToken: String,
    ): List<EmergencyContact> {
        failure?.let { throw it }
        return emergencyContacts
    }

    override suspend fun fetchProfileTeams(
        bearerToken: String,
    ): List<TeamAssociation> {
        failure?.let { throw it }
        return profileTeams
    }

    override suspend fun fetchProfileClubs(
        bearerToken: String,
    ): List<Club> {
        failure?.let { throw it }
        return profileClubs
    }

    override suspend fun fetchTeamHub(bearerToken: String, teamId: String): TeamHub {
        failure?.let { throw it }
        teamHubRequest = bearerToken to teamId
        return teamHub
    }

    override suspend fun fetchMatchDetails(
        bearerToken: String,
        matchId: String,
    ): Fixture {
        failure?.let { throw it }
        matchDetailsRequest = bearerToken to matchId
        return matchDetails
    }
}
