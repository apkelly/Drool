package com.github.apkelly.dribl.api

import com.github.apkelly.dribl.dto.CreateRefereeAvailabilityRequest
import com.github.apkelly.dribl.model.Account
import com.github.apkelly.dribl.model.Club
import com.github.apkelly.dribl.model.EmergencyContact
import com.github.apkelly.dribl.model.Fixture
import com.github.apkelly.dribl.model.LinkMemberRequestResult
import com.github.apkelly.dribl.model.MatchSheet
import com.github.apkelly.dribl.model.MatchSheetGoal
import com.github.apkelly.dribl.model.MatchSheetGoalDraft
import com.github.apkelly.dribl.model.MatchSheetMember
import com.github.apkelly.dribl.model.MatchSheetScore
import com.github.apkelly.dribl.model.Profile
import com.github.apkelly.dribl.model.RefereeAvailability
import com.github.apkelly.dribl.model.RefereeClub
import com.github.apkelly.dribl.model.RelatedUser
import com.github.apkelly.dribl.model.Team
import com.github.apkelly.dribl.model.TeamAssociation
import com.github.apkelly.dribl.model.TeamHub
import com.github.apkelly.dribl.model.TeamSheetAction
import com.github.apkelly.dribl.model.TeamSheetStatus
import io.ktor.client.HttpClient
import kotlin.time.Clock

class DriblSportsApi(
    client: HttpClient,
    baseUrl: String = "https://api.dribl.com/api",
    currentEpochMillis: () -> Long = {
        Clock.System.now().toEpochMilliseconds()
    },
) : SportsRemoteDataSource {
    private val transport = DriblHttpTransport(client, baseUrl)
    private val discoveryApi = DriblDiscoveryApi(transport)
    private val fixtureApi = DriblFixtureApi(transport, currentEpochMillis)
    private val refereeApi = DriblRefereeApi(transport, currentEpochMillis)
    private val matchSheetApi = DriblMatchSheetApi(transport)
    private val profileApi = DriblProfileApi(transport)

    override suspend fun fetchClubs(
        bearerToken: String,
    ): List<Club> = discoveryApi.fetchClubs(bearerToken)

    override suspend fun fetchTeams(
        bearerToken: String,
        clubId: String?,
    ): List<Team> = discoveryApi.fetchTeams(bearerToken, clubId)

    override suspend fun fetchFixtures(
        bearerToken: String,
        userId: String?,
    ): List<Fixture> = fixtureApi.fetchFixtures(bearerToken, userId)

    override suspend fun fetchRefereeFixtures(
        bearerToken: String,
        userId: String,
    ): List<Fixture> = refereeApi.fetchRefereeFixtures(bearerToken, userId)

    override suspend fun updateRefereeAllocation(
        bearerToken: String,
        allocationId: String,
        status: String,
    ) = refereeApi.updateRefereeAllocation(bearerToken, allocationId, status)

    override suspend fun fetchActiveRefereeClubs(
        bearerToken: String,
    ): List<RefereeClub> = refereeApi.fetchActiveRefereeClubs(bearerToken)

    override suspend fun fetchRefereeAvailabilities(
        bearerToken: String,
        userId: String,
        startDate: String,
    ): List<RefereeAvailability> = refereeApi.fetchRefereeAvailabilities(
        bearerToken,
        userId,
        startDate,
    )

    override suspend fun createRefereeAvailability(
        bearerToken: String,
        request: CreateRefereeAvailabilityRequest,
    ): RefereeAvailability = refereeApi.createRefereeAvailability(bearerToken, request)

    override suspend fun deleteRefereeAvailability(
        bearerToken: String,
        availabilityId: String,
    ) = refereeApi.deleteRefereeAvailability(bearerToken, availabilityId)

    override suspend fun fetchTeamHub(
        bearerToken: String,
        teamId: String,
        competitionName: String?,
        leagueId: String?,
    ): TeamHub = fixtureApi.fetchTeamHub(
        bearerToken,
        teamId,
        competitionName,
        leagueId,
    )

    override suspend fun fetchMatchDetails(
        bearerToken: String,
        matchId: String,
    ): Fixture = fixtureApi.fetchMatchDetails(bearerToken, matchId)

    override suspend fun fetchMatchSheet(
        bearerToken: String,
        fixture: Fixture,
    ): MatchSheet = matchSheetApi.fetchMatchSheet(bearerToken, fixture)

    override suspend fun updateMatchSheetMemberAvailability(
        bearerToken: String,
        memberId: String,
        available: Boolean,
    ): MatchSheetMember = matchSheetApi.updateMatchSheetMemberAvailability(
        bearerToken,
        memberId,
        available,
    )

    override suspend fun updateTeamSheetStatus(
        bearerToken: String,
        matchSheetId: String,
        teamId: String,
        action: TeamSheetAction,
    ): TeamSheetStatus = matchSheetApi.updateTeamSheetStatus(
        bearerToken,
        matchSheetId,
        teamId,
        action,
    )

    override suspend fun updateRefereePresence(
        bearerToken: String,
        refereeOfficialId: String,
        checkedIn: Boolean,
    ) = matchSheetApi.updateRefereePresence(
        bearerToken,
        refereeOfficialId,
        checkedIn,
    )

    override suspend fun submitMatchSheetScore(
        bearerToken: String,
        matchSheetId: String,
        scoreId: String?,
        homeHalfTime: Int?,
        awayHalfTime: Int?,
        homeFullTime: Int,
        awayFullTime: Int,
    ): MatchSheetScore = matchSheetApi.submitMatchSheetScore(
        bearerToken,
        matchSheetId,
        scoreId,
        homeHalfTime,
        awayHalfTime,
        homeFullTime,
        awayFullTime,
    )

    override suspend fun createMatchSheetGoal(
        bearerToken: String,
        matchSheetId: String,
        draft: MatchSheetGoalDraft,
    ): MatchSheetGoal = matchSheetApi.createMatchSheetGoal(
        bearerToken,
        matchSheetId,
        draft,
    )

    override suspend fun deleteMatchSheetGoal(
        bearerToken: String,
        goalId: String,
    ) = matchSheetApi.deleteMatchSheetGoal(
        bearerToken,
        goalId,
    )

    override suspend fun fetchProfile(
        bearerToken: String,
    ): Profile = profileApi.fetchProfile(bearerToken)

    override suspend fun fetchRelatedUsers(
        bearerToken: String,
        email: String,
    ): List<RelatedUser> = profileApi.fetchRelatedUsers(bearerToken, email)

    override suspend fun fetchAccounts(
        bearerToken: String,
    ): List<Account> = profileApi.fetchAccounts(bearerToken)

    override suspend fun fetchLinkedUsers(
        bearerToken: String,
    ): List<RelatedUser> = profileApi.fetchLinkedUsers(bearerToken)

    override suspend fun fetchLinkCandidates(
        bearerToken: String,
    ): List<RelatedUser> = profileApi.fetchLinkCandidates(bearerToken)

    override suspend fun requestLinkedUser(
        bearerToken: String,
        candidateId: String,
        isGuardian: Boolean,
    ): LinkMemberRequestResult = profileApi.requestLinkedUser(
        bearerToken,
        candidateId,
        isGuardian,
    )

    override suspend fun verifyLinkedUser(
        bearerToken: String,
        candidateId: String,
        token: String,
    ) = profileApi.verifyLinkedUser(bearerToken, candidateId, token)

    override suspend fun unlinkLinkedUser(
        bearerToken: String,
        linkId: String,
    ) = profileApi.unlinkLinkedUser(bearerToken, linkId)

    override suspend fun createProfileSession(
        bearerToken: String,
        userId: String,
    ): String = profileApi.createProfileSession(bearerToken, userId)

    override suspend fun fetchPersonalInformation(
        bearerToken: String,
        userId: String,
    ): RelatedUser = profileApi.fetchPersonalInformation(bearerToken, userId)

    override suspend fun fetchEmergencyContacts(
        bearerToken: String,
    ): List<EmergencyContact> = profileApi.fetchEmergencyContacts(bearerToken)

    override suspend fun fetchProfileTeams(
        bearerToken: String,
    ): List<TeamAssociation> = profileApi.fetchProfileTeams(bearerToken)

    override suspend fun fetchProfileClubs(
        bearerToken: String,
    ): List<Club> = profileApi.fetchProfileClubs(bearerToken)
}
