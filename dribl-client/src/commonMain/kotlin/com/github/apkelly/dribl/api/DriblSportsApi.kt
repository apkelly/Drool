package com.github.apkelly.dribl.api

import com.github.apkelly.dribl.dto.AccountDto
import com.github.apkelly.dribl.dto.ClubListResponse
import com.github.apkelly.dribl.dto.ClubResourceDto
import com.github.apkelly.dribl.dto.CreateRefereeAvailabilityRequest
import com.github.apkelly.dribl.dto.EmergencyContactResourceDto
import com.github.apkelly.dribl.dto.ImpersonationResponse
import com.github.apkelly.dribl.dto.LadderDetailResponse
import com.github.apkelly.dribl.dto.LadderListResponse
import com.github.apkelly.dribl.dto.LinkUserRequest
import com.github.apkelly.dribl.dto.LinkUserSelection
import com.github.apkelly.dribl.dto.MatchListResponse
import com.github.apkelly.dribl.dto.MatchResourceDto
import com.github.apkelly.dribl.dto.MatchResponse
import com.github.apkelly.dribl.dto.MatchSheetMemberAvailabilityRequest
import com.github.apkelly.dribl.dto.MatchSheetMemberResourceDto
import com.github.apkelly.dribl.dto.MatchSheetOfficialResourceDto
import com.github.apkelly.dribl.dto.MemberCardsResponse
import com.github.apkelly.dribl.dto.PersonDto
import com.github.apkelly.dribl.dto.PersonResourceDto
import com.github.apkelly.dribl.dto.ProfileResponse
import com.github.apkelly.dribl.dto.RefereeAvailabilityListResponse
import com.github.apkelly.dribl.dto.RefereeAvailabilityResourceDto
import com.github.apkelly.dribl.dto.RefereeAvailabilityResponse
import com.github.apkelly.dribl.dto.ResourceList
import com.github.apkelly.dribl.dto.ResourceResponse
import com.github.apkelly.dribl.dto.ScheduleAllocationResourceDto
import com.github.apkelly.dribl.dto.ShortcutResponse
import com.github.apkelly.dribl.dto.TeamListResponse
import com.github.apkelly.dribl.dto.TeamSheetSubmissionRequest
import com.github.apkelly.dribl.dto.TeamSheetSubmissionResourceDto
import com.github.apkelly.dribl.dto.TokenVerificationRequest
import com.github.apkelly.dribl.dto.UpdateRefereeAllocationRequest
import com.github.apkelly.dribl.dto.UserResponse
import com.github.apkelly.dribl.mapper.accountFromDto
import com.github.apkelly.dribl.mapper.avatar
import com.github.apkelly.dribl.mapper.displayName
import com.github.apkelly.dribl.mapper.emergencyContactFromDto
import com.github.apkelly.dribl.mapper.fields
import com.github.apkelly.dribl.mapper.identifier
import com.github.apkelly.dribl.mapper.ladderEntryFromDto
import com.github.apkelly.dribl.mapper.profile
import com.github.apkelly.dribl.mapper.relatedUserFromPerson
import com.github.apkelly.dribl.mapper.relatedUserFromResource
import com.github.apkelly.dribl.mapper.teamIdentifier
import com.github.apkelly.dribl.mapper.toDomain
import com.github.apkelly.dribl.mapper.toTeamSheetStatus
import com.github.apkelly.dribl.model.Account
import com.github.apkelly.dribl.model.Club
import com.github.apkelly.dribl.model.EmergencyContact
import com.github.apkelly.dribl.model.Fixture
import com.github.apkelly.dribl.model.FixtureStatus
import com.github.apkelly.dribl.model.LadderEntry
import com.github.apkelly.dribl.model.LinkMemberRequestResult
import com.github.apkelly.dribl.model.MatchSheet
import com.github.apkelly.dribl.model.MatchSheetMember
import com.github.apkelly.dribl.model.MatchSheetOfficial
import com.github.apkelly.dribl.model.MatchSheetTeam
import com.github.apkelly.dribl.model.Profile
import com.github.apkelly.dribl.model.RefereeAvailability
import com.github.apkelly.dribl.model.RefereeClub
import com.github.apkelly.dribl.model.RelatedUser
import com.github.apkelly.dribl.model.Team
import com.github.apkelly.dribl.model.TeamAssociation
import com.github.apkelly.dribl.model.TeamHub
import com.github.apkelly.dribl.model.TeamRelationship
import com.github.apkelly.dribl.model.TeamSheetAction
import com.github.apkelly.dribl.model.TeamSheetStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Clock
import kotlin.time.Instant

private const val DISCOVER_PAGE_SIZE = 1_000

@Suppress("LargeClass")
class DriblSportsApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.dribl.com/api",
    private val currentEpochMillis: () -> Long = {
        Clock.System.now().toEpochMilliseconds()
    },
) : SportsRemoteDataSource {
    override suspend fun fetchClubs(
        bearerToken: String,
    ): List<Club> {
        val clubs = mutableListOf<Club>()
        var page = 1
        var lastPage: Int
        do {
            val response = getBody<ClubListResponse>("/universal/clubs", bearerToken) {
                parameter("page", page)
                parameter("per_page", DISCOVER_PAGE_SIZE)
            }
            val mapped = response.data.mapIndexedNotNull { index, resource ->
                resource.attributes.copy(id = resource.id).toDomain(index)
            }
            if (response.data.isNotEmpty() && mapped.isEmpty()) {
                throw DriblResponseException("Club response contained no valid clubs")
            }
            clubs += mapped
            lastPage = response.meta?.lastPage ?: 1
            page += 1
        } while (page <= lastPage)
        return clubs.distinctBy(Club::id)
    }

    override suspend fun fetchTeams(
        bearerToken: String,
        clubId: String?,
    ): List<Team> {
        val teams = mutableListOf<Team>()
        var page = 1
        var lastPage: Int
        do {
            val response = getBody<TeamListResponse>("/universal/teams", bearerToken) {
                clubId?.let {
                    parameter("club_id", it)
                    parameter("current_season", true)
                }
                parameter("page", page)
                parameter("per_page", DISCOVER_PAGE_SIZE)
                parameter("sort", "+club,+age_group,+group")
            }
            val mapped = response.data.mapIndexedNotNull { index, resource ->
                resource.attributes.copy(id = resource.id).toDomain(index)
            }
            if (response.data.isNotEmpty() && mapped.isEmpty()) {
                throw DriblResponseException("Team response contained no valid teams")
            }
            teams += mapped
            lastPage = if (clubId == null) 1 else response.meta?.lastPage ?: 1
            page += 1
        } while (page <= lastPage)
        return teams.distinctBy(Team::id)
    }

    override suspend fun fetchFixtures(
        bearerToken: String,
        userId: String?,
    ): List<Fixture> {
        val response = getBody<MatchListResponse>("/universal/matches", bearerToken) {
            userId?.let { parameter("user_id", it) }
            parameter("start_date", upcomingScheduleStart(currentEpochMillis()))
            parameter("remove_byes", true)
            parameter("require_adherance", true)
            parameter("sort", "+date,+home_team")
        }
        return response.data.map(MatchResourceDto::toDomain)
    }

    override suspend fun fetchRefereeFixtures(
        bearerToken: String,
        userId: String,
    ): List<Fixture> {
        val response = getBody<ResourceList<ScheduleAllocationResourceDto>>(
            "/universal/allocations/",
            bearerToken,
        ) {
            parameter("start_date", upcomingScheduleStart(currentEpochMillis()))
            parameter("end_date", refereeScheduleEnd(currentEpochMillis()))
            parameter("user_id", userId)
        }
        val fixtures = response.items.mapNotNull(ScheduleAllocationResourceDto::toDomain)
        if (response.items.isNotEmpty() && fixtures.isEmpty()) {
            throw DriblResponseException("Allocation response contained no valid fixtures")
        }
        return fixtures
    }

    override suspend fun updateRefereeAllocation(
        bearerToken: String,
        allocationId: String,
        status: String,
    ) {
        val url = url("/universal/allocations/$allocationId")
        client.patch(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(UpdateRefereeAllocationRequest(status))
        }.requireSuccess(url)
    }

    override suspend fun fetchActiveRefereeClubs(
        bearerToken: String,
    ): List<RefereeClub> {
        val response = getBody<ResourceList<ClubResourceDto>>(
            "/universal/active-referee-clubs",
            bearerToken,
        )
        return response.items.mapNotNull { resource ->
            val club = resource.attributes
            val id = (club.id ?: club.clubId ?: resource.id).textValue
                ?: return@mapNotNull null
            val name = club.name ?: club.clubName ?: club.code
                ?: return@mapNotNull null
            RefereeClub(
                id = id,
                name = name,
                logoUrl = club.logoUrl ?: club.image,
                timeZone = club.settings?.timeZone ?: "Australia/Sydney",
            )
        }.distinctBy(RefereeClub::id)
    }

    override suspend fun fetchRefereeAvailabilities(
        bearerToken: String,
        userId: String,
        startDate: String,
    ): List<RefereeAvailability> = getBody<RefereeAvailabilityListResponse>(
        "/universal/availabilities",
        bearerToken,
    ) {
        parameter("start_date", startDate)
        parameter("direction", "asc")
        parameter("user_id", userId)
    }.data.mapNotNull(RefereeAvailabilityResourceDto::toDomain)

    override suspend fun createRefereeAvailability(
        bearerToken: String,
        request: CreateRefereeAvailabilityRequest,
    ): RefereeAvailability {
        val endpoint = "/universal/availabilities"
        val url = url(endpoint)
        val response = client.post(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(request)
        }.requireBody<RefereeAvailabilityResponse>(url)
        return response.data.toDomain()
            ?: throw DriblResponseException(
                "Availability response did not contain valid dates",
            )
    }

    override suspend fun deleteRefereeAvailability(
        bearerToken: String,
        availabilityId: String,
    ) {
        val url = url("/universal/availabilities/$availabilityId")
        client.delete(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
        }.requireSuccess(url)
    }

    override suspend fun fetchTeamHub(
        bearerToken: String,
        teamId: String,
        competitionName: String?,
        leagueId: String?,
    ): TeamHub = coroutineScope {
        val matchesRequest = async {
            getBody<MatchListResponse>(
                "/universal/matches",
                bearerToken,
            ) {
                leagueId?.let { parameter("league_id", it) }
                parameter("remove_byes", true)
                parameter("require_adherance", true)
                parameter("sort", "+date,+home_team")
            }
        }
        val laddersRequest = async {
            getBody<LadderListResponse>("/universal/ladders", bearerToken)
        }
        var fixtures = matchesRequest.await().data
            .map(MatchResourceDto::toDomain)
            .filter { it.involves(teamId) }

        val laddersResponse = laddersRequest.await()
        val availableLadders = laddersResponse.ladders.ifEmpty { laddersResponse.data }
        val (preferredLadders, remainingLadders) = availableLadders.partition {
            it.name?.trim().equals(competitionName?.trim(), ignoreCase = true)
        }
        val ladders = preferredLadders + remainingLadders
        var selectedName: String? = null
        var selectedEntries = emptyList<LadderEntry>()
        val identifiedLadders = ladders.mapNotNull { ladder ->
            ladder.id.textValue?.let { ladderId -> ladder to ladderId }
        }
        for ((ladder, ladderId) in identifiedLadders) {
            val detail = getBody<LadderDetailResponse>(
                "/universal/ladders/$ladderId",
                bearerToken,
            ) {
                parameter("require_pools", true)
                parameter("require_form", true)
                parameter("require_adjustments", true)
                parameter("require_championship", true)
            }
            val entries = detail.ladderEntries
                .mapIndexedNotNull { index, entry ->
                    ladderEntryFromDto(index, entry, ladder.name)
                }
            if (entries.any { it.teamId == teamId }) {
                selectedName = ladder.name
                selectedEntries = entries
                val selected = detail.ladderEntries.firstOrNull {
                    it.fields().teamIdentifier == teamId
                }?.fields()
                val formFixtures = (
                    selected?.upcomingMatches.orEmpty() +
                        selected?.recentMatches.orEmpty()
                    ).mapIndexedNotNull { index, fixture ->
                    fixture.toDomain(index, competitionName = ladder.name)
                }
                fixtures = (fixtures + formFixtures)
                    .filter { it.involves(teamId) }
                    .distinctBy(Fixture::id)
                break
            }
        }
        val (results, matches) = fixtures.partition {
            it.status == FixtureStatus.Completed ||
                (it.homeScore != null && it.awayScore != null)
        }
        TeamHub(
            teamId = teamId,
            matches = matches.sortedBy(Fixture::kickoffEpochMillis),
            results = results.sortedByDescending(Fixture::kickoffEpochMillis),
            ladderName = selectedName,
            ladder = selectedEntries,
        )
    }

    override suspend fun fetchMatchDetails(
        bearerToken: String,
        matchId: String,
    ): Fixture {
        val response = getBody<MatchResponse>(
            "/universal/matches/$matchId",
            bearerToken,
        ) {
            parameter("require_season", true)
            parameter("require_adherance", true)
        }
        return response.data.toDomain()
    }

    override suspend fun fetchMatchSheet(
        bearerToken: String,
        fixture: Fixture,
    ): MatchSheet = coroutineScope {
        val matchSheetId = fixture.matchSheetId?.takeIf(String::isNotBlank)
            ?: throw DriblResponseException("Match response did not contain a matchsheet ID")
        val homeTeamId = fixture.homeTeamId?.takeIf(String::isNotBlank)
            ?: throw DriblResponseException("Match response did not contain a home team ID")
        val awayTeamId = fixture.awayTeamId?.takeIf(String::isNotBlank)
            ?: throw DriblResponseException("Match response did not contain an away team ID")
        val homeMembersRequest = async {
            fetchMatchSheetMembers(bearerToken, matchSheetId, homeTeamId)
        }
        val awayMembersRequest = async {
            fetchMatchSheetMembers(bearerToken, matchSheetId, awayTeamId)
        }
        val homeStatusRequest = async {
            fetchTeamSheetStatus(bearerToken, matchSheetId, homeTeamId)
        }
        val awayStatusRequest = async {
            fetchTeamSheetStatus(bearerToken, matchSheetId, awayTeamId)
        }
        val refereesRequest = async {
            fetchMatchSheetOfficials(
                path = "/matchsheet-referees",
                bearerToken = bearerToken,
                fixture = fixture,
                matchSheetId = matchSheetId,
            )
        }
        val officialsRequest = async {
            fetchMatchSheetOfficials(
                path = "/matchsheet-officials",
                bearerToken = bearerToken,
                fixture = fixture,
                matchSheetId = matchSheetId,
            )
        }
        MatchSheet(
            id = matchSheetId,
            status = null,
            homeTeam = MatchSheetTeam(
                id = homeTeamId,
                name = fixture.homeTeamName,
                status = homeStatusRequest.await(),
            ),
            awayTeam = MatchSheetTeam(
                id = awayTeamId,
                name = fixture.awayTeamName,
                status = awayStatusRequest.await(),
            ),
            members = homeMembersRequest.await() + awayMembersRequest.await(),
            referees = refereesRequest.await(),
            officials = officialsRequest.await(),
        )
    }

    override suspend fun updateMatchSheetMemberAvailability(
        bearerToken: String,
        memberId: String,
        available: Boolean,
    ): MatchSheetMember {
        val url = url("/matchsheet-members/$memberId")
        val response = client.put(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(
                MatchSheetMemberAvailabilityRequest(
                    available = available,
                    starting = false.takeUnless { available },
                    playing = false.takeUnless { available },
                    isCaptain = false.takeUnless { available },
                    isGoalkeeper = false.takeUnless { available },
                ),
            )
        }.requireBody<ResourceResponse<MatchSheetMemberResourceDto>>(url)
        return response.item.toDomain()
    }

    override suspend fun updateTeamSheetStatus(
        bearerToken: String,
        matchSheetId: String,
        teamId: String,
        action: TeamSheetAction,
    ): TeamSheetStatus {
        val url = url("/matchsheet-team-submissions")
        val response = client.patch(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(
                TeamSheetSubmissionRequest(
                    matchSheetId = matchSheetId,
                    teamId = teamId,
                    type = action.name.lowercase(),
                ),
            )
        }.requireBody<ResourceResponse<TeamSheetSubmissionResourceDto>>(url)
        return response.item.attributes.status.toTeamSheetStatus()
    }

    private suspend fun fetchMatchSheetMembers(
        bearerToken: String,
        matchSheetId: String,
        teamId: String,
    ): List<MatchSheetMember> = getBody<ResourceList<MatchSheetMemberResourceDto>>(
        "/matchsheet-members",
        bearerToken,
    ) {
        parameter("matchsheet_id", matchSheetId)
        parameter("team_id", teamId)
    }.items.map(MatchSheetMemberResourceDto::toDomain)

    private suspend fun fetchTeamSheetStatus(
        bearerToken: String,
        matchSheetId: String,
        teamId: String,
    ): TeamSheetStatus = getBody<ResourceResponse<TeamSheetSubmissionResourceDto>>(
        "/matchsheet-team-submissions/$matchSheetId/$teamId",
        bearerToken,
    ).item.attributes.status.toTeamSheetStatus()

    private suspend fun fetchMatchSheetOfficials(
        path: String,
        bearerToken: String,
        fixture: Fixture,
        matchSheetId: String,
    ): List<MatchSheetOfficial> = getBody<ResourceList<MatchSheetOfficialResourceDto>>(
        path,
        bearerToken,
    ) {
        parameter("matchsheet_id", matchSheetId)
        fixture.tenantId?.let { parameter("tenant_id", it) }
        fixture.seasonId?.let { parameter("season_id", it) }
        if (path.endsWith("referees")) parameter("require_user_id", true)
    }.items.map(MatchSheetOfficialResourceDto::toDomain)

    override suspend fun fetchProfile(
        bearerToken: String,
    ): Profile {
        val response = getBody<ProfileResponse>("/auth/related-users", bearerToken)
        val profile = response.profile()
            ?: throw DriblResponseException("Profile response did not contain a profile")
        val accountId = profile.identifier()
            ?: throw DriblResponseException(
                "Profile response did not contain an account identifier",
            )
        return Profile(
            accountId = accountId,
            displayName = profile.displayName(accountId),
            email = profile.email ?: profile.emailAddress,
            avatarUrl = profile.avatar(),
        )
    }

    override suspend fun fetchRelatedUsers(
        bearerToken: String,
        email: String,
    ): List<RelatedUser> {
        val response = getBody<ResourceList<PersonDto>>(
            "/auth/related-users",
            bearerToken,
        ) {
            parameter("email", email)
        }
        return response.items.mapIndexedNotNull { index, person ->
            relatedUserFromPerson(index, person, isLinked = false)
        }
    }

    override suspend fun fetchAccounts(
        bearerToken: String,
    ): List<Account> {
        val response = getBody<ResourceList<AccountDto>>(
            "/access/accounts",
            bearerToken,
        )
        return response.items.mapIndexed(::accountFromDto)
    }

    override suspend fun fetchLinkedUsers(
        bearerToken: String,
    ): List<RelatedUser> {
        val response = getBody<ResourceList<PersonResourceDto>>(
            "/linked-users",
            bearerToken,
        )
        return response.items
            .filter {
                it.attributes.status == null ||
                    it.attributes.status.equals("approved", ignoreCase = true)
            }
            .mapIndexedNotNull { index, resource ->
                relatedUserFromResource(index, resource, isLinked = true)
            }
    }

    override suspend fun fetchLinkCandidates(
        bearerToken: String,
    ): List<RelatedUser> {
        val response = getBody<ResourceList<PersonDto>>(
            "/linked-users-lookup",
            bearerToken,
        )
        return response.items.mapIndexed { index, person ->
            relatedUserFromPerson(index, person)
        }
    }

    override suspend fun requestLinkedUser(
        bearerToken: String,
        candidateId: String,
        isGuardian: Boolean,
    ): LinkMemberRequestResult {
        val url = url("/linked-users")
        val response = client.post(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(
                LinkUserRequest(
                    relatedUser = LinkUserSelection(candidateId, isGuardian),
                    approve = true,
                ),
            )
        }.requireBody<UserResponse>(url)
        val linkedUser = response.data
            ?: throw DriblResponseException("Link user response did not contain relationship data")
        val relationshipId = linkedUser.id.textValue
            ?: throw DriblResponseException("Link user response did not contain a relationship ID")
        return LinkMemberRequestResult(
            relationshipId = relationshipId,
            isApproved = linkedUser.attributes.status.equals("approved", ignoreCase = true),
        )
    }

    override suspend fun verifyLinkedUser(
        bearerToken: String,
        candidateId: String,
        token: String,
    ) {
        val url = url("/linked-users/$candidateId")
        client.patch(url) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            setBody(TokenVerificationRequest(token))
        }.requireSuccess(url)
    }

    override suspend fun unlinkLinkedUser(
        bearerToken: String,
        linkId: String,
    ) {
        val url = url("/linked-users/$linkId")
        client.delete(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
        }.requireSuccess(url)
    }

    override suspend fun createProfileSession(
        bearerToken: String,
        userId: String,
    ): String {
        val url = url("/auth/impersonate/$userId")
        val response = client.post(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
        }.requireBody<ImpersonationResponse>(url)
        return response.token
            ?: throw DriblResponseException(
                "Profile session response did not contain token",
            )
    }

    override suspend fun fetchPersonalInformation(
        bearerToken: String,
        userId: String,
    ): RelatedUser {
        val response = getBody<UserResponse>("/users/$userId", bearerToken)
        val user = response.data
            ?: throw DriblResponseException(
                "Personal information response did not contain a user",
            )
        return relatedUserFromResource(0, user, isLinked = true)
    }

    override suspend fun fetchEmergencyContacts(
        bearerToken: String,
    ): List<EmergencyContact> {
        val response = getBody<ResourceList<EmergencyContactResourceDto>>(
            "/user-contacts/",
            bearerToken,
        )
        return response.items.mapIndexedNotNull { index, resource ->
            emergencyContactFromDto(index, resource.attributes)
        }
    }

    override suspend fun fetchProfileTeams(
        bearerToken: String,
    ): List<TeamAssociation> {
        val response = getBody<ShortcutResponse>("/access/shortcut", bearerToken) {
            parameter("status", "active")
            parameter("require_color", true)
        }
        return response.teams.mapIndexedNotNull { index, resource ->
            val attributes = resource.attributes
            val team = attributes.copy(id = resource.id).toDomain(index)
                ?: return@mapIndexedNotNull null
            val roleSlugs = (attributes.roles + resource.roles)
                .flatMap {
                    listOfNotNull(
                        it.attributes.slug?.lowercase(),
                        it.attributes.name?.lowercase(),
                    )
                }
            TeamAssociation(
                team = team,
                relationship = roleSlugs.toTeamRelationship(),
            )
        }
    }

    private fun List<String>.toTeamRelationship(): TeamRelationship = when {
        any { "manager" in it } -> TeamRelationship.Managing
        any { "coach" in it } -> TeamRelationship.Coaching
        any { "player" in it } -> TeamRelationship.Player
        any { "supporter" in it } -> TeamRelationship.Following
        else -> TeamRelationship.None
    }

    override suspend fun fetchProfileClubs(
        bearerToken: String,
    ): List<Club> {
        val response = getBody<MemberCardsResponse>(
            "/universal/member-cards",
            bearerToken,
        )
        return response.teams.mapIndexedNotNull { index, resource ->
            val attributes = resource.attributes
            val name = attributes.clubName ?: return@mapIndexedNotNull null
            Club(
                id = attributes.clubId.textValue ?: "club-$index-$name",
                name = name,
                shortName = null,
                logoUrl = attributes.image,
                primaryColor = attributes.color,
                secondaryColor = attributes.accent,
            )
        }.distinctBy(Club::id)
    }

    private suspend inline fun <reified T> getBody(
        path: String,
        bearerToken: String,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val url = url(path)
        return client.get(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, ORIGINAL_DRIBL_USER_AGENT)
            bearerAuth(bearerToken)
            configure()
        }.requireBody(url)
    }

    private fun url(
        path: String,
    ): String = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}

private fun upcomingScheduleStart(
    currentEpochMillis: Long,
): String = Instant.fromEpochMilliseconds(currentEpochMillis - SCHEDULE_LOOKBACK_MILLIS).toString()

private fun refereeScheduleEnd(
    currentEpochMillis: Long,
): String = Instant.fromEpochMilliseconds(
    currentEpochMillis + REFEREE_SCHEDULE_LOOKAHEAD_MILLIS,
).toString()

private fun Fixture.involves(
    teamId: String,
): Boolean = homeTeamId == teamId || awayTeamId == teamId || userTeamId == teamId

private val JsonPrimitive?.textValue: String?
    get() = this?.contentOrNull?.takeUnless { it.isBlank() || it == "null" }

private const val SCHEDULE_LOOKBACK_MILLIS = 14L * 24L * 60L * 60L * 1_000L
private const val REFEREE_SCHEDULE_LOOKAHEAD_MILLIS = 90L * 24L * 60L * 60L * 1_000L
