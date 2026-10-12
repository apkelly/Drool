package com.github.apkelly.drool.common.data.remote

import com.github.apkelly.dribl.dto.CreateRefereeAvailabilityRequest
import com.github.apkelly.drool.common.domain.model.Account
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.EmergencyContact
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.LinkMemberRequestResult
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.RefereeAvailability
import com.github.apkelly.drool.common.domain.model.RefereeClub
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.Team
import com.github.apkelly.drool.common.domain.model.TeamAssociation
import com.github.apkelly.drool.common.domain.model.TeamHub

interface SportsRemoteDataSource : MatchSheetRemoteDataSource {
    suspend fun fetchClubs(
        bearerToken: String,
    ): List<Club>

    suspend fun fetchTeams(
        bearerToken: String,
        clubId: String? = null,
    ): List<Team>

    suspend fun fetchFixtures(
        bearerToken: String,
        userId: String? = null,
    ): List<Fixture>

    suspend fun fetchRefereeFixtures(
        bearerToken: String,
        userId: String,
    ): List<Fixture>

    suspend fun updateRefereeAllocation(
        bearerToken: String,
        allocationId: String,
        status: String,
    )

    suspend fun fetchActiveRefereeClubs(
        bearerToken: String,
    ): List<RefereeClub>

    suspend fun fetchRefereeAvailabilities(
        bearerToken: String,
        userId: String,
        startDate: String,
    ): List<RefereeAvailability>

    suspend fun createRefereeAvailability(
        bearerToken: String,
        request: CreateRefereeAvailabilityRequest,
    ): RefereeAvailability

    suspend fun deleteRefereeAvailability(
        bearerToken: String,
        availabilityId: String,
    )

    suspend fun fetchProfile(
        bearerToken: String,
    ): Profile

    suspend fun fetchRelatedUsers(
        bearerToken: String,
        email: String,
    ): List<RelatedUser>

    suspend fun fetchAccounts(
        bearerToken: String,
    ): List<Account>

    suspend fun fetchLinkedUsers(
        bearerToken: String,
    ): List<RelatedUser>

    suspend fun fetchLinkCandidates(
        bearerToken: String,
    ): List<RelatedUser>

    suspend fun requestLinkedUser(
        bearerToken: String,
        candidateId: String,
        isGuardian: Boolean,
    ): LinkMemberRequestResult

    suspend fun verifyLinkedUser(
        bearerToken: String,
        candidateId: String,
        token: String,
    )

    suspend fun unlinkLinkedUser(
        bearerToken: String,
        linkId: String,
    )

    suspend fun createProfileSession(
        bearerToken: String,
        userId: String,
    ): String

    suspend fun fetchPersonalInformation(
        bearerToken: String,
        userId: String,
    ): RelatedUser

    suspend fun fetchEmergencyContacts(
        bearerToken: String,
    ): List<EmergencyContact>

    suspend fun fetchProfileTeams(
        bearerToken: String,
    ): List<TeamAssociation>

    suspend fun fetchProfileClubs(
        bearerToken: String,
    ): List<Club>

    suspend fun fetchTeamHub(
        bearerToken: String,
        teamId: String,
        competitionName: String? = null,
        leagueId: String? = null,
    ): TeamHub

    suspend fun fetchMatchDetails(
        bearerToken: String,
        matchId: String,
    ): Fixture
}
