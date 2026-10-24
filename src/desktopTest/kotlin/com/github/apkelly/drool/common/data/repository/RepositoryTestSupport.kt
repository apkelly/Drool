package com.github.apkelly.drool.common.data.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.apkelly.dribl.dto.AuthApiRequest
import com.github.apkelly.dribl.dto.CreateRefereeAvailabilityRequest
import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.remote.AuthRemoteDataSource
import com.github.apkelly.drool.common.data.remote.SportsRemoteDataSource
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.data.storage.createBearerTokenStore
import com.github.apkelly.drool.common.domain.model.Account
import com.github.apkelly.drool.common.domain.model.AuthenticationResult
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.EmergencyContact
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import com.github.apkelly.drool.common.domain.model.LinkMemberRequestResult
import com.github.apkelly.drool.common.domain.model.MatchSheet
import com.github.apkelly.drool.common.domain.model.MatchSheetGoal
import com.github.apkelly.drool.common.domain.model.MatchSheetGoalDraft
import com.github.apkelly.drool.common.domain.model.MatchSheetMember
import com.github.apkelly.drool.common.domain.model.MatchSheetScore
import com.github.apkelly.drool.common.domain.model.MatchSheetTeam
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.RefereeAvailability
import com.github.apkelly.drool.common.domain.model.RefereeClub
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.Team
import com.github.apkelly.drool.common.domain.model.TeamAssociation
import com.github.apkelly.drool.common.domain.model.TeamHub
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
import com.github.apkelly.drool.common.domain.model.TeamSheetStatus
import kotlinx.coroutines.Dispatchers
import java.io.File

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
    ): AuthenticationResult {
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
    var matchSheet = MatchSheet(
        id = "sheet",
        status = "pending",
        homeTeam = MatchSheetTeam("home", "Home", TeamSheetStatus.Open),
        awayTeam = MatchSheetTeam("away", "Away", TeamSheetStatus.Open),
        members = emptyList(),
        referees = emptyList(),
        officials = emptyList(),
    )
    var matchSheetRequest: Pair<String, String>? = null
    var memberAvailabilityRequest: Triple<String, String, Boolean>? = null
    var teamSheetStatusRequest: List<String>? = null
    var teamHubRequest: Pair<String, String>? = null
    var teamHubCompetitionName: String? = null
    var teamHubLeagueId: String? = null
    var teamHubCalls = 0
    var matchDetailsRequest: Pair<String, String>? = null
    var refereeClubs: List<RefereeClub> = emptyList()
    var refereeClubsByToken: Map<String, List<RefereeClub>> = emptyMap()
    var refereeFixtures: List<Fixture> = emptyList()
    var refereeFixturesByUser: Map<String, List<Fixture>> = emptyMap()
    var refereeAvailabilities: List<RefereeAvailability> = emptyList()
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
    ): List<Club> {
        failure?.let { throw it }
        return clubs.map { it }
    }

    override suspend fun fetchTeams(
        bearerToken: String,
        clubId: String?,
    ): List<Team> {
        failure?.let { throw it }
        teamClubId = clubId
        return teams.map { it }
    }

    override suspend fun fetchFixtures(
        bearerToken: String,
        userId: String?,
    ): List<Fixture> {
        failure?.let { throw it }
        fixtureUserIds += userId
        return fixtures.map { it }
    }

    override suspend fun fetchRefereeFixtures(
        bearerToken: String,
        userId: String,
    ): List<Fixture> {
        failure?.let { throw it }
        refereeFixtureTokens += bearerToken
        refereeFixtureUserIds += userId
        return (refereeFixturesByUser[userId] ?: refereeFixtures).map { it }
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
    ): Profile {
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
        return relatedUsers.map { it }
    }

    override suspend fun fetchAccounts(
        bearerToken: String,
    ): List<Account> {
        accountsCalls++
        associationFailure?.let { throw it }
        failure?.let { throw it }
        return accounts.map { it }
    }

    override suspend fun fetchLinkedUsers(
        bearerToken: String,
    ): List<RelatedUser> {
        linkedUsersCalls++
        associationFailure?.let { throw it }
        failure?.let { throw it }
        return (linkedUsers ?: relatedUsers).map { it }
    }

    override suspend fun fetchLinkCandidates(
        bearerToken: String,
    ): List<RelatedUser> {
        failure?.let { throw it }
        return linkCandidates.map { it }
    }

    override suspend fun requestLinkedUser(
        bearerToken: String,
        candidateId: String,
        isGuardian: Boolean,
    ): LinkMemberRequestResult {
        failure?.let { throw it }
        requestedLink = candidateId to isGuardian
        return LinkMemberRequestResult(
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
    ): RelatedUser {
        failure?.let { throw it }
        return (
            personalInformationDto ?: RelatedUser(
                id = userId,
                displayName = userId,
                email = null,
                avatarUrl = null,
            )
            )
    }

    override suspend fun fetchEmergencyContacts(
        bearerToken: String,
    ): List<EmergencyContact> {
        failure?.let { throw it }
        return emergencyContacts.map { it }
    }

    override suspend fun fetchProfileTeams(
        bearerToken: String,
    ): List<TeamAssociation> {
        failure?.let { throw it }
        return profileTeams.map { it }
    }

    override suspend fun fetchProfileClubs(
        bearerToken: String,
    ): List<Club> {
        failure?.let { throw it }
        return profileClubs.map { it }
    }

    override suspend fun fetchTeamHub(
        bearerToken: String,
        teamId: String,
        competitionName: String?,
        leagueId: String?,
    ): TeamHub {
        failure?.let { throw it }
        teamHubCalls += 1
        teamHubRequest = bearerToken to teamId
        teamHubCompetitionName = competitionName
        teamHubLeagueId = leagueId
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

    override suspend fun fetchMatchSheet(
        bearerToken: String,
        fixture: Fixture,
    ): MatchSheet {
        failure?.let { throw it }
        matchSheetRequest = bearerToken to fixture.id
        return matchSheet
    }

    override suspend fun updateMatchSheetMemberAvailability(
        bearerToken: String,
        memberId: String,
        available: Boolean,
    ): MatchSheetMember {
        failure?.let { throw it }
        memberAvailabilityRequest = Triple(bearerToken, memberId, available)
        return matchSheet.members.first { it.id == memberId }.copy(available = available)
    }

    override suspend fun updateTeamSheetStatus(
        bearerToken: String,
        matchSheetId: String,
        teamId: String,
        action: TeamSheetAction,
    ): TeamSheetStatus {
        failure?.let { throw it }
        teamSheetStatusRequest = listOf(
            bearerToken,
            matchSheetId,
            teamId,
            action.name,
        )
        return when (action) {
            TeamSheetAction.Submit -> TeamSheetStatus.Submitted

            TeamSheetAction.Confirm -> TeamSheetStatus.Confirmed

            TeamSheetAction.Unsubmit,
            TeamSheetAction.Unconfirm,
            -> TeamSheetStatus.Open
        }
    }

    override suspend fun fetchActiveRefereeClubs(
        bearerToken: String,
    ): List<RefereeClub> {
        failure?.let { throw it }
        return refereeClubsByToken[bearerToken] ?: refereeClubs
    }

    override suspend fun fetchRefereeAvailabilities(
        bearerToken: String,
        userId: String,
        startDate: String,
    ): List<RefereeAvailability> {
        failure?.let { throw it }
        refereeAvailabilityQuery = userId to startDate
        return refereeAvailabilities
    }

    override suspend fun createRefereeAvailability(
        bearerToken: String,
        request: CreateRefereeAvailabilityRequest,
    ): RefereeAvailability {
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

    var refereePresenceRequest: Triple<String, String, Boolean>? = null
    var submittedScoreRequest: MatchSheetScore? = null
    val createdGoals = mutableListOf<MatchSheetGoalDraft>()
    val deletedGoalIds = mutableListOf<String>()

    override suspend fun updateRefereePresence(
        bearerToken: String,
        refereeOfficialId: String,
        checkedIn: Boolean,
    ) {
        failure?.let { throw it }
        refereePresenceRequest = Triple(bearerToken, refereeOfficialId, checkedIn)
    }

    override suspend fun submitMatchSheetScore(
        bearerToken: String,
        matchSheetId: String,
        scoreId: String?,
        homeHalfTime: Int?,
        awayHalfTime: Int?,
        homeFullTime: Int,
        awayFullTime: Int,
    ): MatchSheetScore {
        failure?.let { throw it }
        val score = MatchSheetScore(
            id = scoreId ?: "score-1",
            homeHalfTime = homeHalfTime,
            awayHalfTime = awayHalfTime,
            homeFullTime = homeFullTime,
            awayFullTime = awayFullTime,
        )
        submittedScoreRequest = score
        return score
    }

    override suspend fun createMatchSheetGoal(
        bearerToken: String,
        matchSheetId: String,
        draft: MatchSheetGoalDraft,
    ): MatchSheetGoal {
        failure?.let { throw it }
        createdGoals += draft
        return MatchSheetGoal(
            id = "goal-${createdGoals.size}",
            matchSheetMemberId = draft.matchSheetMemberId,
            half = draft.half,
            minute = draft.minute,
            ownGoal = draft.ownGoal,
            penaltyKick = draft.penaltyKick,
        )
    }

    override suspend fun deleteMatchSheetGoal(
        bearerToken: String,
        goalId: String,
    ) {
        failure?.let { throw it }
        deletedGoalIds += goalId
    }
}
