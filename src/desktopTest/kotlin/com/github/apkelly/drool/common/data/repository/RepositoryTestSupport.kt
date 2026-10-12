package com.github.apkelly.drool.common.data.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.apkelly.dribl.api.AuthRemoteDataSource
import com.github.apkelly.dribl.api.SportsRemoteDataSource
import com.github.apkelly.dribl.model.AuthApiRequest
import com.github.apkelly.dribl.model.CreateRefereeAvailabilityRequest
import com.github.apkelly.dribl.model.LinkUserResult
import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.data.storage.createBearerTokenStore
import com.github.apkelly.drool.common.domain.model.Account
import com.github.apkelly.drool.common.domain.model.AuthenticationResult
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.EmergencyContact
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.Team
import com.github.apkelly.drool.common.domain.model.TeamAssociation
import com.github.apkelly.drool.common.domain.model.TeamHub
import kotlinx.coroutines.Dispatchers
import java.io.File
import com.github.apkelly.dribl.model.Account as DriblAccount
import com.github.apkelly.dribl.model.AuthenticationResult as DriblAuthenticationResult
import com.github.apkelly.dribl.model.Club as DriblClub
import com.github.apkelly.dribl.model.EmergencyContact as DriblEmergencyContact
import com.github.apkelly.dribl.model.Fixture as DriblFixture
import com.github.apkelly.dribl.model.Profile as DriblProfile
import com.github.apkelly.dribl.model.RefereeAvailability as DriblRefereeAvailability
import com.github.apkelly.dribl.model.RefereeClub as DriblRefereeClub
import com.github.apkelly.dribl.model.RelatedUser as DriblRelatedUser
import com.github.apkelly.dribl.model.Team as DriblTeam
import com.github.apkelly.dribl.model.TeamAssociation as DriblTeamAssociation
import com.github.apkelly.dribl.model.TeamHub as DriblTeamHub

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

    override suspend fun signIn(
        request: AuthApiRequest,
    ): DriblAuthenticationResult {
        failure?.let { throw it }
        this.request = request
        return result.toDribl()
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
    var teamHubCalls = 0
    var matchDetailsRequest: Pair<String, String>? = null
    var refereeClubs: List<DriblRefereeClub> = emptyList()
    var refereeClubsByToken: Map<String, List<DriblRefereeClub>> = emptyMap()
    var refereeFixtures: List<Fixture> = emptyList()
    var refereeAvailabilities: List<DriblRefereeAvailability> = emptyList()
    var refereeAvailabilityQuery: Pair<String, String>? = null
    var createAvailabilityRequest: CreateRefereeAvailabilityRequest? = null
    var deletedAvailabilityId: String? = null
    var linkCandidates: List<RelatedUser> = emptyList()
    var requestedLink: Pair<String, Boolean>? = null
    var linkRequestApproved = false
    var verifiedLink: Pair<String, String>? = null
    var unlinkedUserId: String? = null
    val profileSessionUsers = mutableListOf<String>()
    val profileSessionFailureUsers = mutableSetOf<String>()
    val fixtureUserIds = mutableListOf<String?>()
    val refereeFixtureTokens = mutableListOf<String>()
    val refereeFixtureUserIds = mutableListOf<String>()
    var updatedRefereeAllocation: Triple<String, String, String>? = null
    var teamClubId: String? = null
    var relatedUsersCalls = 0
    var relatedUsersEmail: String? = null
    var accountsCalls = 0
    var linkedUsersCalls = 0
    var failure: Exception? = null
    var associationFailure: Exception? = null

    override suspend fun fetchClubs(
        bearerToken: String,
    ): List<DriblClub> {
        failure?.let { throw it }
        return clubs.map { it.toDribl() }
    }

    override suspend fun fetchTeams(
        bearerToken: String,
        clubId: String?,
    ): List<DriblTeam> {
        failure?.let { throw it }
        teamClubId = clubId
        return teams.map { it.toDribl() }
    }

    override suspend fun fetchFixtures(
        bearerToken: String,
        userId: String?,
    ): List<DriblFixture> {
        failure?.let { throw it }
        fixtureUserIds += userId
        return fixtures.map { it.toDribl() }
    }

    override suspend fun fetchRefereeFixtures(
        bearerToken: String,
        userId: String,
    ): List<DriblFixture> {
        failure?.let { throw it }
        refereeFixtureTokens += bearerToken
        refereeFixtureUserIds += userId
        return refereeFixtures.map { it.toDribl() }
    }

    override suspend fun updateRefereeAllocation(
        bearerToken: String,
        allocationId: String,
        status: String,
    ) {
        failure?.let { throw it }
        updatedRefereeAllocation = Triple(bearerToken, allocationId, status)
    }

    override suspend fun fetchProfile(
        bearerToken: String,
    ): DriblProfile {
        failure?.let { throw it }
        return profile.toDribl()
    }

    override suspend fun fetchRelatedUsers(
        bearerToken: String,
        email: String,
    ): List<DriblRelatedUser> {
        relatedUsersCalls++
        relatedUsersEmail = email
        associationFailure?.let { throw it }
        failure?.let { throw it }
        return relatedUsers.map { it.toDribl() }
    }

    override suspend fun fetchAccounts(
        bearerToken: String,
    ): List<DriblAccount> {
        accountsCalls++
        associationFailure?.let { throw it }
        failure?.let { throw it }
        return accounts.map { it.toDribl() }
    }

    override suspend fun fetchLinkedUsers(
        bearerToken: String,
    ): List<DriblRelatedUser> {
        linkedUsersCalls++
        associationFailure?.let { throw it }
        failure?.let { throw it }
        return (linkedUsers ?: relatedUsers).map { it.toDribl() }
    }

    override suspend fun fetchLinkCandidates(
        bearerToken: String,
    ): List<DriblRelatedUser> {
        failure?.let { throw it }
        return linkCandidates.map { it.toDribl() }
    }

    override suspend fun requestLinkedUser(
        bearerToken: String,
        candidateId: String,
        isGuardian: Boolean,
    ): LinkUserResult {
        failure?.let { throw it }
        requestedLink = candidateId to isGuardian
        return LinkUserResult(
            relationshipId = "linked-$candidateId",
            isApproved = linkRequestApproved,
        )
    }

    override suspend fun verifyLinkedUser(
        bearerToken: String,
        candidateId: String,
        token: String,
    ) {
        failure?.let { throw it }
        verifiedLink = candidateId to token
    }

    override suspend fun unlinkLinkedUser(
        bearerToken: String,
        linkId: String,
    ) {
        failure?.let { throw it }
        unlinkedUserId = linkId
        linkedUsers = linkedUsers.orEmpty().filterNot { it.linkId == linkId }
    }

    override suspend fun createProfileSession(
        bearerToken: String,
        userId: String,
    ): String {
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
    ): DriblRelatedUser {
        failure?.let { throw it }
        return (
            personalInformationDto ?: RelatedUser(
                id = userId,
                displayName = userId,
                email = null,
                avatarUrl = null,
            )
            ).toDribl()
    }

    override suspend fun fetchEmergencyContacts(
        bearerToken: String,
    ): List<DriblEmergencyContact> {
        failure?.let { throw it }
        return emergencyContacts.map { it.toDribl() }
    }

    override suspend fun fetchProfileTeams(
        bearerToken: String,
    ): List<DriblTeamAssociation> {
        failure?.let { throw it }
        return profileTeams.map { it.toDribl() }
    }

    override suspend fun fetchProfileClubs(
        bearerToken: String,
    ): List<DriblClub> {
        failure?.let { throw it }
        return profileClubs.map { it.toDribl() }
    }

    override suspend fun fetchTeamHub(
        bearerToken: String,
        teamId: String,
    ): DriblTeamHub {
        failure?.let { throw it }
        teamHubCalls += 1
        teamHubRequest = bearerToken to teamId
        return teamHub.toDribl()
    }

    override suspend fun fetchMatchDetails(
        bearerToken: String,
        matchId: String,
    ): DriblFixture {
        failure?.let { throw it }
        matchDetailsRequest = bearerToken to matchId
        return matchDetails.toDribl()
    }

    override suspend fun fetchActiveRefereeClubs(
        bearerToken: String,
    ): List<DriblRefereeClub> {
        failure?.let { throw it }
        return refereeClubsByToken[bearerToken] ?: refereeClubs
    }

    override suspend fun fetchRefereeAvailabilities(
        bearerToken: String,
        userId: String,
        startDate: String,
    ): List<DriblRefereeAvailability> {
        failure?.let { throw it }
        refereeAvailabilityQuery = userId to startDate
        return refereeAvailabilities
    }

    override suspend fun createRefereeAvailability(
        bearerToken: String,
        request: CreateRefereeAvailabilityRequest,
    ): DriblRefereeAvailability {
        failure?.let { throw it }
        createAvailabilityRequest = request
        return refereeAvailabilities.first()
    }

    override suspend fun deleteRefereeAvailability(
        bearerToken: String,
        availabilityId: String,
    ) {
        failure?.let { throw it }
        deletedAvailabilityId = availabilityId
    }
}

private fun AuthenticationResult.toDribl() = DriblAuthenticationResult(
    bearerToken = bearerToken,
    identity = identity?.let {
        com.github.apkelly.dribl.model.AuthenticationIdentity(
            id = it.id,
            firstName = it.firstName,
            lastName = it.lastName,
            email = it.email,
            avatarUrl = it.avatarUrl,
        )
    },
    relatedUsers = relatedUsers.map { it.toDribl() },
    accounts = accounts.map { it.toDribl() },
)

private fun Profile.toDribl() = DriblProfile(
    accountId = accountId,
    displayName = displayName,
    email = email,
    avatarUrl = avatarUrl,
    relatedUsers = relatedUsers.map { it.toDribl() },
    accounts = accounts.map { it.toDribl() },
)

private fun RelatedUser.toDribl() = DriblRelatedUser(
    id = id,
    displayName = displayName,
    email = email,
    avatarUrl = avatarUrl,
    subjectUserId = subjectUserId,
    linkId = linkId,
    isLinked = isLinked,
    gender = gender,
    dateOfBirth = dateOfBirth,
    phoneNumber = phoneNumber,
    address = address,
    emergencyContacts = emergencyContacts.map { it.toDribl() },
    firstName = firstName,
    isGuardian = isGuardian,
)

private fun EmergencyContact.toDribl() = DriblEmergencyContact(
    name = name,
    phoneNumber = phoneNumber,
    email = email,
)

private fun Account.toDribl() = DriblAccount(
    id = id,
    name = name,
    subtitle = subtitle,
    logoUrl = logoUrl,
)

private fun Club.toDribl() = DriblClub(
    id = id,
    name = name,
    shortName = shortName,
    logoUrl = logoUrl,
    primaryColor = primaryColor,
    secondaryColor = secondaryColor,
)

private fun Team.toDribl() = DriblTeam(
    id = id,
    clubId = clubId,
    name = name,
    shortName = shortName,
    logoUrl = logoUrl,
    ageGroup = ageGroup,
    competitionName = competitionName,
    active = active,
    primaryColor = primaryColor,
    secondaryColor = secondaryColor,
)

private fun TeamAssociation.toDribl() = DriblTeamAssociation(
    team = team.toDribl(),
    relationship = com.github.apkelly.dribl.model.TeamRelationship.valueOf(
        relationship.name,
    ),
)

private fun Fixture.toDribl() = DriblFixture(
    id = id,
    profileId = profileId,
    kickoffEpochMillis = kickoffEpochMillis,
    homeTeamId = homeTeamId,
    homeTeamName = homeTeamName,
    awayTeamId = awayTeamId,
    awayTeamName = awayTeamName,
    competitionName = competitionName,
    venueName = venueName,
    userTeamId = userTeamId,
    status = com.github.apkelly.dribl.model.FixtureStatus.valueOf(status.name),
    homeScore = homeScore,
    awayScore = awayScore,
    homeTeamLogoUrl = homeTeamLogoUrl,
    awayTeamLogoUrl = awayTeamLogoUrl,
    roundLabel = roundLabel,
    venueAddress = venueAddress,
    latitude = latitude,
    longitude = longitude,
    role = com.github.apkelly.dribl.model.FixtureRole.valueOf(role.name),
    refereeRole = refereeRole,
    leagueName = leagueName,
)

private fun TeamHub.toDribl() = DriblTeamHub(
    teamId = teamId,
    matches = matches.map { it.toDribl() },
    results = results.map { it.toDribl() },
    ladderName = ladderName,
    ladder = ladder.map {
        com.github.apkelly.dribl.model.LadderEntry(
            position = it.position,
            teamId = it.teamId,
            teamName = it.teamName,
            logoUrl = it.logoUrl,
            played = it.played,
            won = it.won,
            drawn = it.drawn,
            lost = it.lost,
            goalsFor = it.goalsFor,
            goalsAgainst = it.goalsAgainst,
            goalDifference = it.goalDifference,
            points = it.points,
        )
    },
)
