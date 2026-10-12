package com.github.apkelly.drool.common.data.remote

import com.github.apkelly.dribl.dto.CreateRefereeAvailabilityRequest
import com.github.apkelly.drool.common.data.mapper.toDomain
import com.github.apkelly.drool.common.data.mapper.toDribl
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.MatchSheet
import com.github.apkelly.drool.common.domain.model.MatchSheetMember
import com.github.apkelly.drool.common.domain.model.RefereeAvailability
import com.github.apkelly.drool.common.domain.model.TeamSheetAction
import com.github.apkelly.drool.common.domain.model.TeamSheetStatus
import com.github.apkelly.dribl.model.TeamSheetAction as DriblTeamSheetAction

internal class DriblSportsRemoteDataSource(
    private val delegate: com.github.apkelly.dribl.api.SportsRemoteDataSource,
) : SportsRemoteDataSource {
    override suspend fun fetchClubs(
        bearerToken: String,
    ) = delegate.fetchClubs(bearerToken).map { it.toDomain() }

    override suspend fun fetchTeams(
        bearerToken: String,
        clubId: String?,
    ) = delegate.fetchTeams(bearerToken, clubId).map { it.toDomain() }

    override suspend fun fetchFixtures(
        bearerToken: String,
        userId: String?,
    ) = delegate.fetchFixtures(bearerToken, userId).map { it.toDomain() }

    override suspend fun fetchRefereeFixtures(
        bearerToken: String,
        userId: String,
    ) = delegate.fetchRefereeFixtures(bearerToken, userId).map { it.toDomain() }

    override suspend fun updateRefereeAllocation(
        bearerToken: String,
        allocationId: String,
        status: String,
    ) = delegate.updateRefereeAllocation(bearerToken, allocationId, status)

    override suspend fun fetchActiveRefereeClubs(
        bearerToken: String,
    ) = delegate.fetchActiveRefereeClubs(bearerToken).map { it.toDomain() }

    override suspend fun fetchRefereeAvailabilities(
        bearerToken: String,
        userId: String,
        startDate: String,
    ) = delegate.fetchRefereeAvailabilities(bearerToken, userId, startDate).map {
        it.toDomain()
    }

    override suspend fun createRefereeAvailability(
        bearerToken: String,
        request: CreateRefereeAvailabilityRequest,
    ): RefereeAvailability = delegate.createRefereeAvailability(bearerToken, request).toDomain()

    override suspend fun deleteRefereeAvailability(
        bearerToken: String,
        availabilityId: String,
    ) = delegate.deleteRefereeAvailability(bearerToken, availabilityId)

    override suspend fun fetchProfile(
        bearerToken: String,
    ) = delegate.fetchProfile(bearerToken).toDomain()

    override suspend fun fetchRelatedUsers(
        bearerToken: String,
        email: String,
    ) = delegate.fetchRelatedUsers(bearerToken, email).map { it.toDomain() }

    override suspend fun fetchAccounts(
        bearerToken: String,
    ) = delegate.fetchAccounts(bearerToken).map { it.toDomain() }

    override suspend fun fetchLinkedUsers(
        bearerToken: String,
    ) = delegate.fetchLinkedUsers(bearerToken).map { it.toDomain() }

    override suspend fun fetchLinkCandidates(
        bearerToken: String,
    ) = delegate.fetchLinkCandidates(bearerToken).map { it.toDomain() }

    override suspend fun requestLinkedUser(
        bearerToken: String,
        candidateId: String,
        isGuardian: Boolean,
    ) = delegate.requestLinkedUser(bearerToken, candidateId, isGuardian).toDomain()

    override suspend fun verifyLinkedUser(
        bearerToken: String,
        candidateId: String,
        token: String,
    ) = delegate.verifyLinkedUser(bearerToken, candidateId, token)

    override suspend fun unlinkLinkedUser(
        bearerToken: String,
        linkId: String,
    ) = delegate.unlinkLinkedUser(bearerToken, linkId)

    override suspend fun createProfileSession(
        bearerToken: String,
        userId: String,
    ) = delegate.createProfileSession(bearerToken, userId)

    override suspend fun fetchPersonalInformation(
        bearerToken: String,
        userId: String,
    ) = delegate.fetchPersonalInformation(bearerToken, userId).toDomain()

    override suspend fun fetchEmergencyContacts(
        bearerToken: String,
    ) = delegate.fetchEmergencyContacts(bearerToken).map { it.toDomain() }

    override suspend fun fetchProfileTeams(
        bearerToken: String,
    ) = delegate.fetchProfileTeams(bearerToken).map { it.toDomain() }

    override suspend fun fetchProfileClubs(
        bearerToken: String,
    ) = delegate.fetchProfileClubs(bearerToken).map { it.toDomain() }

    override suspend fun fetchTeamHub(
        bearerToken: String,
        teamId: String,
        competitionName: String?,
        leagueId: String?,
    ) = delegate.fetchTeamHub(bearerToken, teamId, competitionName, leagueId).toDomain()

    override suspend fun fetchMatchDetails(
        bearerToken: String,
        matchId: String,
    ) = delegate.fetchMatchDetails(bearerToken, matchId).toDomain()

    override suspend fun fetchMatchSheet(
        bearerToken: String,
        fixture: Fixture,
    ): MatchSheet = delegate.fetchMatchSheet(bearerToken, fixture.toDribl()).toDomain()

    override suspend fun updateMatchSheetMemberAvailability(
        bearerToken: String,
        memberId: String,
        available: Boolean,
    ): MatchSheetMember = delegate
        .updateMatchSheetMemberAvailability(bearerToken, memberId, available)
        .toDomain()

    override suspend fun updateTeamSheetStatus(
        bearerToken: String,
        matchSheetId: String,
        teamId: String,
        action: TeamSheetAction,
    ): TeamSheetStatus = TeamSheetStatus.valueOf(
        delegate.updateTeamSheetStatus(
            bearerToken,
            matchSheetId,
            teamId,
            DriblTeamSheetAction.valueOf(action.name),
        ).name,
    )
}
