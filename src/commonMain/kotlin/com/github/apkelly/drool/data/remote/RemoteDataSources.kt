package com.github.apkelly.drool.data.remote

import com.github.apkelly.drool.data.remote.model.AuthApiRequest
import com.github.apkelly.drool.domain.model.Account
import com.github.apkelly.drool.domain.model.AuthenticationResult
import com.github.apkelly.drool.domain.model.Club
import com.github.apkelly.drool.domain.model.EmergencyContact
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.RelatedUser
import com.github.apkelly.drool.domain.model.Team
import com.github.apkelly.drool.domain.model.TeamAssociation
import com.github.apkelly.drool.domain.model.TeamHub

interface AuthRemoteDataSource {
    suspend fun signIn(request: AuthApiRequest): AuthenticationResult
}

interface SportsRemoteDataSource {
    suspend fun fetchClubs(bearerToken: String): List<Club>
    suspend fun fetchTeams(bearerToken: String, clubId: String? = null): List<Team>
    suspend fun fetchFixtures(
        bearerToken: String,
        userId: String? = null,
    ): List<Fixture>
    suspend fun fetchRefereeFixtures(
        bearerToken: String,
        userId: String,
    ): List<Fixture>
    suspend fun fetchProfile(bearerToken: String): Profile
    suspend fun fetchRelatedUsers(bearerToken: String, email: String): List<RelatedUser>
    suspend fun fetchAccounts(bearerToken: String): List<Account>
    suspend fun fetchLinkedUsers(bearerToken: String): List<RelatedUser>
    suspend fun fetchLinkCandidates(bearerToken: String): List<RelatedUser>
    suspend fun verifyLinkedUser(bearerToken: String, candidateId: String, token: String)
    suspend fun createProfileSession(bearerToken: String, userId: String): String
    suspend fun fetchPersonalInformation(
        bearerToken: String,
        userId: String,
    ): RelatedUser
    suspend fun fetchEmergencyContacts(bearerToken: String): List<EmergencyContact>
    suspend fun fetchProfileTeams(bearerToken: String): List<TeamAssociation>
    suspend fun fetchProfileClubs(bearerToken: String): List<Club>
    suspend fun fetchTeamHub(bearerToken: String, teamId: String): TeamHub
    suspend fun fetchMatchDetails(bearerToken: String, matchId: String): Fixture
}
