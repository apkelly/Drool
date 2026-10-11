package com.github.apkelly.dribl.api

import com.github.apkelly.dribl.dto.AccountDto
import com.github.apkelly.dribl.dto.ClubDto
import com.github.apkelly.dribl.dto.ClubListResponse
import com.github.apkelly.dribl.dto.ClubResourceDto
import com.github.apkelly.dribl.dto.EmergencyContactDto
import com.github.apkelly.dribl.dto.EmergencyContactResourceDto
import com.github.apkelly.dribl.dto.FixtureDto
import com.github.apkelly.dribl.dto.ImpersonationResponse
import com.github.apkelly.dribl.dto.LadderDetailResponse
import com.github.apkelly.dribl.dto.LadderEntryDto
import com.github.apkelly.dribl.dto.LadderEntryResourceDto
import com.github.apkelly.dribl.dto.LadderListResponse
import com.github.apkelly.dribl.dto.LinkUserRequest
import com.github.apkelly.dribl.dto.LinkUserSelection
import com.github.apkelly.dribl.dto.MatchAttributesDto
import com.github.apkelly.dribl.dto.MatchListResponse
import com.github.apkelly.dribl.dto.MatchResourceDto
import com.github.apkelly.dribl.dto.MatchResponse
import com.github.apkelly.dribl.dto.MatchRoleAttributesDto
import com.github.apkelly.dribl.dto.MatchRoleResourceDto
import com.github.apkelly.dribl.dto.MemberCardsResponse
import com.github.apkelly.dribl.dto.PersonDto
import com.github.apkelly.dribl.dto.PersonResourceDto
import com.github.apkelly.dribl.dto.ProfileResponse
import com.github.apkelly.dribl.dto.RefereeAvailabilityListResponse
import com.github.apkelly.dribl.dto.RefereeAvailabilityResourceDto
import com.github.apkelly.dribl.dto.RefereeAvailabilityResponse
import com.github.apkelly.dribl.dto.ResourceList
import com.github.apkelly.dribl.dto.ScheduleAllocationResourceDto
import com.github.apkelly.dribl.dto.ScheduleResponse
import com.github.apkelly.dribl.dto.ShortcutResponse
import com.github.apkelly.dribl.dto.TeamDto
import com.github.apkelly.dribl.dto.TeamListResponse
import com.github.apkelly.dribl.dto.TokenVerificationRequest
import com.github.apkelly.dribl.dto.UserResponse
import com.github.apkelly.dribl.model.Account
import com.github.apkelly.dribl.model.AvailabilityPeriod
import com.github.apkelly.dribl.model.Club
import com.github.apkelly.dribl.model.CreateRefereeAvailabilityRequest
import com.github.apkelly.dribl.model.EmergencyContact
import com.github.apkelly.dribl.model.Fixture
import com.github.apkelly.dribl.model.FixtureRole
import com.github.apkelly.dribl.model.FixtureStatus
import com.github.apkelly.dribl.model.LadderEntry
import com.github.apkelly.dribl.model.LinkUserResult
import com.github.apkelly.dribl.model.Profile
import com.github.apkelly.dribl.model.RefereeAvailability
import com.github.apkelly.dribl.model.RefereeClub
import com.github.apkelly.dribl.model.RelatedUser
import com.github.apkelly.dribl.model.Team
import com.github.apkelly.dribl.model.TeamAssociation
import com.github.apkelly.dribl.model.TeamHub
import com.github.apkelly.dribl.model.TeamRelationship
import com.github.apkelly.dribl.model.normalizedTeamName
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
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.time.Clock
import kotlin.time.Instant

private const val DISCOVER_PAGE_SIZE = 1_000

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
                resource.attributes.copy(id = resource.id).toDto(index)
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
                resource.attributes.copy(id = resource.id).toDto(index)
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
        val response = getBody<ScheduleResponse>("/universal/schedule", bearerToken) {
            parameter("start_date", upcomingScheduleStart(currentEpochMillis()))
            parameter("direction", "asc")
            parameter("user_id", userId)
            parameter("skip_first", false)
            parameter("require_payrun", false)
        }
        return response.allocations.mapNotNull(ScheduleAllocationResourceDto::toDomain)
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
    ): TeamHub {
        val matchesResponse = getBody<MatchListResponse>(
            "/universal/matches",
            bearerToken,
        ) {
            parameter("remove_byes", true)
            parameter("require_adherance", true)
            parameter("sort", "+date,+home_team")
        }
        var fixtures = matchesResponse.data
            .map(MatchResourceDto::toDomain)
            .filter { it.involves(teamId) }

        val laddersResponse =
            getBody<LadderListResponse>("/universal/ladders", bearerToken)
        val ladders = laddersResponse.ladders.ifEmpty { laddersResponse.data }
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
                    fixture.toDto(index, competitionName = ladder.name)
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
        return TeamHub(
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
    ): LinkUserResult {
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
        return LinkUserResult(
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
            val team = attributes.copy(id = resource.id).toDto(index)
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

class DriblHttpException(
    val statusCode: Int,
    url: String,
    responseBody: String,
) : IllegalStateException(
    "HTTP $statusCode for $url\n${responseBody.take(MAX_ERROR_BODY_LENGTH)}",
)

class DriblResponseException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

private fun clubFromDto(
    index: Int,
    club: ClubDto,
): Club? = club.toDto(index)

private fun ClubDto.toDto(
    index: Int,
): Club? {
    val resolvedName = name ?: clubName ?: return null
    return Club(
        id = (clubId ?: id).textValue ?: "club-$index-$resolvedName",
        name = resolvedName,
        shortName = shortName ?: code,
        logoUrl = logoUrl ?: image,
        primaryColor = primaryColor ?: color,
        secondaryColor = secondaryColor ?: accent,
    )
}

private fun teamFromDto(
    index: Int,
    team: TeamDto,
): Team? = team.toDto(index)

private fun TeamDto.toDto(
    index: Int,
): Team? {
    val resolvedName = name ?: parsedName ?: teamName ?: return null
    val resolvedCompetitionName = competitionName ?: teamCompetitionName
    return Team(
        id = (teamId ?: id).textValue ?: "team-$index-$resolvedName",
        clubId = clubId.textValue,
        name = normalizedTeamName(resolvedName, resolvedCompetitionName),
        shortName = shortName,
        logoUrl = logoUrl ?: image,
        ageGroup = ageGroup,
        competitionName = resolvedCompetitionName,
        active = active,
        primaryColor = primaryColor ?: color,
        secondaryColor = secondaryColor ?: accent,
    )
}

private fun relatedUserFromResource(
    index: Int,
    resource: PersonResourceDto,
    isLinked: Boolean = false,
): RelatedUser = relatedUserFromPerson(
    index = index,
    person = resource.attributes,
    relationshipId = resource.id.textValue,
    linkId = resource.id.textValue.takeIf { isLinked },
    isLinked = isLinked,
)

private fun ProfileResponse.profile(): PersonDto? = data?.account ?: data?.profile ?: data?.user ?: profile

private fun PersonDto.identifier(): String? = (accountId ?: userId ?: id).textValue

private fun PersonDto.displayName(
    fallback: String,
): String = firstName ?: displayName ?: fullName ?: name ?: email ?: fallback

private fun PersonDto.email(): String? = email ?: emailAddress ?: contactEmail ?: value?.takeIf { it.contains('@') }

private fun PersonDto.identifier(
    index: Int,
    relationshipId: String?,
    userId: String?,
    email: String?,
): String = userId
    ?: relationshipId
    ?: accountId.textValue
    ?: id.textValue
    ?: "related-$index-${email ?: firstName.orEmpty()}"

private fun PersonDto.displayName(
    email: String?,
    fallback: String,
): String = listOfNotNull(firstName, lastName)
    .joinToString(" ")
    .takeIf(String::isNotBlank)
    ?: email
    ?: fallback

private fun PersonDto.avatar(): String? = avatarUrl ?: profileImage ?: image ?: source

private fun PersonDto.phoneNumber(): String? = phone ?: mobile ?: phoneNumber ?: mobileNumber

private fun relatedUserFromPerson(
    index: Int,
    person: PersonDto,
    relationshipId: String? = null,
    linkId: String? = null,
    isLinked: Boolean = false,
): RelatedUser {
    val email = person.email()
    val userId = person.userId.textValue
    val id = person.identifier(index, relationshipId, userId, email)
    return RelatedUser(
        id = id,
        displayName = person.displayName(email, id),
        email = email,
        avatarUrl = person.avatar(),
        subjectUserId = userId ?: id,
        linkId = linkId,
        isLinked = isLinked,
        dateOfBirth = person.dateOfBirth ?: person.alternateDateOfBirth,
        gender = person.gender,
        phoneNumber = person.phoneNumber(),
        firstName = person.firstName,
        address = person.formattedAddress(),
        emergencyContacts = person.emergencyContacts
            .mapIndexedNotNull(::emergencyContactFromDto),
        isGuardian = person.isGuardian?.let { it != 0 },
    )
}

private fun accountFromDto(
    index: Int,
    account: AccountDto,
): Account {
    val id = (account.accountId ?: account.id).textValue ?: "account-$index"
    val attributes = account.attributes
    return Account(
        id = id,
        name = attributes?.name
            ?: attributes?.value
            ?: attributes?.email
            ?: account.accountName
            ?: account.name
            ?: account.value
            ?: id,
        subtitle = attributes?.type
            ?: attributes?.role
            ?: attributes?.organisationName
            ?: account.role
            ?: account.type,
        logoUrl = account.logoUrl ?: account.image,
    )
}

private fun PersonDto.formattedAddress(): String? {
    val value = address
    return listOfNotNull(
        addressLine1 ?: value?.line1,
        addressLine2 ?: value?.line2,
        city ?: value?.city ?: value?.suburb,
        state ?: value?.state,
        postcode ?: value?.postcode,
    ).distinct().joinToString(", ").takeIf(String::isNotBlank)
}

private fun emergencyContactFromDto(
    index: Int,
    contact: EmergencyContactDto,
): EmergencyContact? {
    val phoneNumber = contact.phoneNumber1
        ?: contact.phone
        ?: contact.mobile
        ?: contact.phoneNumber2
    val email = contact.email ?: contact.contactEmail
    if (phoneNumber == null && email == null) return null
    val name = contact.name
        ?: contact.fullName
        ?: contact.contactName
        ?: "Contact ${index + 1}"
    return EmergencyContact(name, phoneNumber, email)
}

private fun ladderEntryFromDto(
    index: Int,
    resource: LadderEntryResourceDto,
    competitionName: String?,
): LadderEntry? {
    val entry = resource.fields()
    val teamName = entry.teamName ?: return null
    return LadderEntry(
        position = entry.position ?: index + 1,
        teamId = entry.teamIdentifier,
        teamName = normalizedTeamName(
            name = teamName,
            competitionName = entry.leagueName ?: competitionName,
            allowEmbeddedLeaguePrefix = true,
        ),
        logoUrl = entry.clubLogo ?: entry.image,
        played = entry.played,
        won = entry.won,
        drawn = entry.drawn,
        lost = entry.lost,
        goalsFor = entry.goalsFor,
        goalsAgainst = entry.goalsAgainst,
        goalDifference = entry.goalDifference,
        points = entry.points,
    )
}

private fun LadderEntryResourceDto.fields(): LadderEntryDto = attributes ?: LadderEntryDto(
    position = position,
    teamHashId = teamId,
    teamName = teamName,
)

private val LadderEntryDto.teamIdentifier: String?
    get() = (teamId ?: teamHashId).textValue

private fun FixtureDto.resolvedHomeName(): String? = homeTeamName ?: homeTeam?.name

private fun FixtureDto.resolvedAwayName(): String? = awayTeamName ?: awayTeam?.name

private fun FixtureDto.resolvedHomeTeamId(): String? =
    (homeTeamId ?: homeTeamHashId ?: homeTeam?.teamId ?: homeTeam?.id).textValue

private fun FixtureDto.resolvedAwayTeamId(): String? =
    (awayTeamId ?: awayTeamHashId ?: awayTeam?.teamId ?: awayTeam?.id).textValue

private fun FixtureDto.resolvedIdentifier(
    index: Int,
    homeName: String,
    awayName: String,
): String = (id ?: matchId).textValue
    ?: "fixture-$index-$homeName-$awayName"

private fun FixtureDto.toDto(
    index: Int,
    competitionName: String? = null,
    venueName: String? = null,
    userTeamId: String? = null,
    status: String? = null,
): Fixture? {
    val resolvedHomeName = resolvedHomeName() ?: return null
    val resolvedAwayName = resolvedAwayName() ?: return null
    val resolvedCompetitionName = this.competitionName ?: leagueName ?: competitionName
    return Fixture(
        id = resolvedIdentifier(index, resolvedHomeName, resolvedAwayName),
        kickoffEpochMillis = (kickoffAt ?: date).epochMillis ?: 0L,
        homeTeamId = resolvedHomeTeamId(),
        homeTeamName = normalizedTeamName(
            name = resolvedHomeName,
            competitionName = resolvedCompetitionName,
            allowEmbeddedLeaguePrefix = this.competitionName == null,
        ),
        awayTeamId = resolvedAwayTeamId(),
        awayTeamName = normalizedTeamName(
            name = resolvedAwayName,
            competitionName = resolvedCompetitionName,
            allowEmbeddedLeaguePrefix = this.competitionName == null,
        ),
        competitionName = resolvedCompetitionName,
        venueName = this.venueName ?: groundName ?: venueName,
        userTeamId = this.userTeamId.textValue ?: userTeamId,
        status = (this.status ?: status).toFixtureStatus(),
        homeScore = homeScore,
        awayScore = awayScore,
        homeTeamLogoUrl = homeTeamLogo ?: homeClubImage,
        awayTeamLogoUrl = awayTeamLogo ?: awayClubImage,
        leagueName = leagueName,
    )
}

private fun MatchResourceDto.toDomain(): Fixture = attributes.toDomain(id)

private fun MatchAttributesDto.toDomain(
    resourceId: JsonPrimitive,
): Fixture {
    val resolvedCompetitionName = competitionName ?: leagueName
    return Fixture(
        id = resourceId.textValue
            ?: throw DriblResponseException("Match response contained a blank id"),
        kickoffEpochMillis = date.epochMillis
            ?: throw DriblResponseException("Match response contained an invalid date"),
        homeTeamId = homeTeamId.textValue,
        homeTeamName = normalizedTeamName(
            name = homeTeamName,
            competitionName = resolvedCompetitionName,
            allowEmbeddedLeaguePrefix = competitionName == null,
        ),
        awayTeamId = awayTeamId.textValue,
        awayTeamName = normalizedTeamName(
            name = awayTeamName,
            competitionName = resolvedCompetitionName,
            allowEmbeddedLeaguePrefix = competitionName == null,
        ),
        competitionName = resolvedCompetitionName,
        venueName = fieldName,
        userTeamId = null,
        status = status.toFixtureStatus(),
        homeScore = homeTeamScore,
        awayScore = awayTeamScore,
        homeTeamLogoUrl = homeTeamLogo,
        awayTeamLogoUrl = awayTeamLogo,
        roundLabel = roundLabel?.takeIf(String::isNotBlank)
            ?: roundNumber.textValue,
        venueAddress = address,
        latitude = latitude?.doubleOrNull,
        longitude = longitude?.doubleOrNull,
        role = roles.fixtureRole(),
        leagueName = leagueName,
    )
}

private val MatchRoleAttributesDto.isReferee: Boolean
    get() = listOfNotNull(name, slug).any {
        it.contains("referee", ignoreCase = true)
    }

private val MatchRoleAttributesDto.isCoach: Boolean
    get() = listOfNotNull(name, slug).any {
        it.contains("coach", ignoreCase = true)
    }

private fun List<MatchRoleResourceDto>.fixtureRole(): FixtureRole = when {
    any { it.attributes.isReferee } -> FixtureRole.Referee
    any { it.attributes.isCoach } -> FixtureRole.Coach
    else -> FixtureRole.Player
}

private fun ScheduleAllocationResourceDto.toDomain(): Fixture? {
    val matchId = attributes.eventId.textValue ?: id.textValue ?: return null
    val kickoff = attributes.date.epochMillis ?: return null
    val homeName = attributes.homeTeamName ?: attributes.homeClubName ?: return null
    val awayName = attributes.awayTeamName ?: attributes.awayClubName ?: return null
    val competition = attributes.competitionName ?: attributes.leagueName
    return Fixture(
        id = matchId,
        kickoffEpochMillis = kickoff,
        homeTeamId = attributes.homeTeamId.textValue,
        homeTeamName = normalizedTeamName(homeName, competition),
        awayTeamId = attributes.awayTeamId.textValue,
        awayTeamName = normalizedTeamName(awayName, competition),
        competitionName = competition,
        venueName = listOfNotNull(attributes.ground, attributes.field)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" - ")
            .takeIf(String::isNotBlank),
        userTeamId = null,
        status = attributes.status.toFixtureStatus().takeUnless {
            it == FixtureStatus.Unknown
        } ?: FixtureStatus.Scheduled,
        homeTeamLogoUrl = attributes.homeClubImage,
        awayTeamLogoUrl = attributes.awayClubImage,
        role = FixtureRole.Referee,
        refereeRole = attributes.refereeRole?.takeIf(String::isNotBlank),
        leagueName = attributes.leagueName,
    )
}

private fun RefereeAvailabilityResourceDto.toDomain(): RefereeAvailability? {
    val startEpochMillis = attributes.start.epochMillis ?: return null
    val endEpochMillis = attributes.end.epochMillis ?: return null
    val club = attributes.clubs.firstOrNull()
    return RefereeAvailability(
        id = id.textValue ?: return null,
        startEpochMillis = startEpochMillis,
        endEpochMillis = endEpochMillis,
        clubId = attributes.clubId.textValue ?: club?.id.textValue,
        clubName = club?.name ?: club?.clubName,
        period = AvailabilityPeriod.entries.firstOrNull {
            it.name.equals(attributes.templatePeriod, ignoreCase = true)
        } ?: AvailabilityPeriod.SingleDay,
        templateEndEpochMillis = attributes.templateEnd.epochMillis,
    )
}

private fun upcomingScheduleStart(
    currentEpochMillis: Long,
): String = Instant.fromEpochMilliseconds(currentEpochMillis - SCHEDULE_LOOKBACK_MILLIS).toString()

private fun Fixture.involves(
    teamId: String,
): Boolean = homeTeamId == teamId || awayTeamId == teamId || userTeamId == teamId

private fun String?.toFixtureStatus(): FixtureStatus = when (orEmpty().lowercase()) {
    "pending" -> FixtureStatus.Pending
    "scheduled", "upcoming", "fixture" -> FixtureStatus.Scheduled
    "live", "in_progress" -> FixtureStatus.Live
    "completed", "complete", "final" -> FixtureStatus.Completed
    "postponed" -> FixtureStatus.Postponed
    "washout" -> FixtureStatus.Washout
    "cancelled", "canceled" -> FixtureStatus.Cancelled
    else -> FixtureStatus.Unknown
}

private val JsonPrimitive?.textValue: String?
    get() = this?.contentOrNull?.takeUnless { it.isBlank() || it == "null" }

private const val SCHEDULE_LOOKBACK_MILLIS = 24L * 60L * 60L * 1_000L
private const val MAX_ERROR_BODY_LENGTH = 2_000

private val JsonPrimitive?.epochMillis: Long?
    get() = this?.longOrNull?.let { raw ->
        if (raw < 100_000_000_000L) raw * 1_000L else raw
    } ?: this?.contentOrNull?.let { value ->
        try {
            Instant.parse(value).toEpochMilliseconds()
        } catch (_: IllegalArgumentException) {
            null
        }
    }
