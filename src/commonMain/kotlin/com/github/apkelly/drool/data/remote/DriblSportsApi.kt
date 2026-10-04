package com.github.apkelly.drool.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
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
import kotlin.time.Instant
import com.github.apkelly.drool.data.remote.dto.AccountDto
import com.github.apkelly.drool.data.remote.dto.ClubListResponse
import com.github.apkelly.drool.data.remote.dto.ClubDto
import com.github.apkelly.drool.data.remote.dto.EmergencyContactDto
import com.github.apkelly.drool.data.remote.dto.EmergencyContactResourceDto
import com.github.apkelly.drool.data.remote.dto.FixtureListResponse
import com.github.apkelly.drool.data.remote.dto.FixtureDto
import com.github.apkelly.drool.data.remote.dto.ImpersonationResponse
import com.github.apkelly.drool.data.remote.dto.LadderDetailResponse
import com.github.apkelly.drool.data.remote.dto.LadderEntryResourceDto
import com.github.apkelly.drool.data.remote.dto.LadderEntryDto
import com.github.apkelly.drool.data.remote.dto.LadderListResponse
import com.github.apkelly.drool.data.remote.dto.MatchAttributesDto
import com.github.apkelly.drool.data.remote.dto.MatchListResponse
import com.github.apkelly.drool.data.remote.dto.MatchResourceDto
import com.github.apkelly.drool.data.remote.dto.MatchResponse
import com.github.apkelly.drool.data.remote.dto.MemberCardsResponse
import com.github.apkelly.drool.data.remote.dto.NestedResourceResponse
import com.github.apkelly.drool.data.remote.dto.PersonResourceDto
import com.github.apkelly.drool.data.remote.dto.PersonDto
import com.github.apkelly.drool.data.remote.dto.ProfileResponse
import com.github.apkelly.drool.data.remote.dto.ResourceList
import com.github.apkelly.drool.data.remote.dto.ShortcutResponse
import com.github.apkelly.drool.data.remote.dto.TeamListResponse
import com.github.apkelly.drool.data.remote.dto.TeamDto
import com.github.apkelly.drool.data.remote.dto.TokenVerificationRequest
import com.github.apkelly.drool.data.remote.dto.UserResponse
import com.github.apkelly.drool.domain.model.Account
import com.github.apkelly.drool.domain.model.Club
import com.github.apkelly.drool.domain.model.EmergencyContact
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FixtureStatus
import com.github.apkelly.drool.domain.model.LadderEntry
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.RelatedUser
import com.github.apkelly.drool.domain.model.Team
import com.github.apkelly.drool.domain.model.TeamAssociation
import com.github.apkelly.drool.domain.model.TeamHub
import com.github.apkelly.drool.domain.model.TeamRelationship

private const val DISCOVER_PAGE_SIZE = 1_000

class DriblSportsApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.dribl.com/api",
) : SportsRemoteDataSource {
    override suspend fun fetchClubs(bearerToken: String): List<Club> {
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

    override suspend fun fetchTeams(bearerToken: String, clubId: String?): List<Team> {
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
        val response = getBody<FixtureListResponse>("/universal/schedule", bearerToken) {
            userId?.let { parameter("user_id", it) }
            parameter("direction", "asc")
            parameter("skip_first", false)
            parameter("require_payrun", false)
        }
        val fixtures = if (response.allocations.isNotEmpty()) {
            response.allocations.mapIndexedNotNull { index, allocation ->
                allocation.fixture?.toDto(
                    index = index,
                    competitionName = allocation.competitionName,
                    venueName = allocation.venueName,
                    userTeamId = allocation.userTeamId,
                    status = allocation.status,
                )
            }
        } else {
            response.matches.ifEmpty { response.data }
                .mapIndexedNotNull { index, fixture -> fixture.toDto(index) }
        }
        return fixtures
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
        for (ladder in ladders) {
            val ladderId = ladder.id.textValue ?: continue
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
                .mapIndexedNotNull(::ladderEntryFromDto)
            if (entries.any { it.teamId == teamId }) {
                selectedName = ladder.name
                selectedEntries = entries
                val selected = detail.ladderEntries.firstOrNull {
                    it.fields().teamIdentifier == teamId
                }?.fields()
                val formFixtures = (
                    selected?.upcomingMatches.orEmpty() +
                        selected?.recentMatches.orEmpty()
                    ).mapIndexedNotNull { index, fixture -> fixture.toDto(index) }
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

    override suspend fun fetchProfile(bearerToken: String): Profile {
        val response = getBody<ProfileResponse>("/auth/related-users", bearerToken)
        val payload = response.data
        val profile = payload?.account ?: payload?.profile ?: payload?.user ?: response.profile
            ?: throw DriblResponseException("Profile response did not contain a profile")
        val accountId = (profile.accountId ?: profile.userId ?: profile.id).textValue
            ?: throw DriblResponseException(
                "Profile response did not contain an account identifier"
            )
        val displayName = profile.firstName
            ?: profile.displayName
            ?: profile.fullName
            ?: profile.name
            ?: profile.email
            ?: accountId
        val teams = payload?.activeTeams.orEmpty() + payload?.activeTeamsSnake.orEmpty()
        return Profile(
            accountId = accountId,
            displayName = displayName,
            email = profile.email ?: profile.emailAddress,
            playingTeamIds = teams.mapNotNull {
                (it.teamId ?: it.teamIdCamel ?: it.id).textValue
            }.toSet(),
            avatarUrl = profile.avatarUrl
                ?: profile.profileImage
                ?: profile.image
                ?: profile.source,
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

    override suspend fun fetchAccounts(bearerToken: String): List<Account> {
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
        val response = getBody<NestedResourceResponse<PersonResourceDto>>(
            "/linked-users-lookup",
            bearerToken,
        )
        return response.data?.items.orEmpty()
            .mapIndexedNotNull(::relatedUserFromResource)
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
            header(HttpHeaders.UserAgent, OriginalDriblUserAgent)
            bearerAuth(bearerToken)
            setBody(TokenVerificationRequest(token))
        }.requireSuccess(url)
    }

    override suspend fun createProfileSession(
        bearerToken: String,
        userId: String,
    ): String {
        val url = url("/auth/impersonate/$userId")
        val response = client.post(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, OriginalDriblUserAgent)
            bearerAuth(bearerToken)
        }.requireBody<ImpersonationResponse>(url)
        return response.token
            ?: throw DriblResponseException(
                "Profile session response did not contain token"
            )
    }

    override suspend fun fetchPersonalInformation(
        bearerToken: String,
        userId: String,
    ): RelatedUser {
        val response = getBody<UserResponse>("/users/$userId", bearerToken)
        val user = response.data
            ?: throw DriblResponseException(
                "Personal information response did not contain a user"
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
            TeamAssociation(
                team = team,
                relationship = if (
                        attributes.roles.any {
                            it.attributes.slug.equals("teamsupporter", ignoreCase = true)
                        }
                    ) {
                        TeamRelationship.Following
                    } else {
                        TeamRelationship.PlaysFor
                    },
            )
        }
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
            header(HttpHeaders.UserAgent, OriginalDriblUserAgent)
            bearerAuth(bearerToken)
            configure()
        }.requireBody(url)
    }

    private fun url(path: String): String =
        baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}

class DriblHttpException(
    val statusCode: Int,
    url: String,
    responseBody: String,
) : IllegalStateException("HTTP $statusCode for $url\n${responseBody.take(2_000)}")

class DriblResponseException(message: String) : IllegalStateException(message)

private fun clubFromDto(index: Int, club: ClubDto): Club? =
    club.toDto(index)

private fun ClubDto.toDto(index: Int): Club? {
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

private fun teamFromDto(index: Int, team: TeamDto): Team? =
    team.toDto(index)

private fun TeamDto.toDto(index: Int): Team? {
    val resolvedName = name ?: parsedName ?: teamName ?: return null
    return Team(
        id = (teamId ?: id).textValue ?: "team-$index-$resolvedName",
        clubId = clubId.textValue,
        name = resolvedName,
        shortName = shortName,
        logoUrl = logoUrl ?: image,
        ageGroup = ageGroup,
        competitionName = competitionName ?: teamCompetitionName,
        active = active,
        primaryColor = primaryColor ?: color,
        secondaryColor = secondaryColor ?: accent,
    )
}

private fun relatedUserFromResource(
    index: Int,
    resource: PersonResourceDto,
    isLinked: Boolean = false,
): RelatedUser =
    relatedUserFromPerson(
        index = index,
        person = resource.attributes,
        relationshipId = resource.id.textValue,
        isLinked = isLinked,
    )

private fun relatedUserFromPerson(
    index: Int,
    person: PersonDto,
    relationshipId: String? = null,
    isLinked: Boolean = false,
): RelatedUser {
    val email = person.email
        ?: person.emailAddress
        ?: person.contactEmail
        ?: person.value?.takeIf { it.contains('@') }
    val userId = person.userId.textValue
    val id = userId
            ?: relationshipId
            ?: person.accountId.textValue
            ?: person.id.textValue
            ?: "related-$index-${email ?: person.firstName.orEmpty()}"
    val displayName = listOfNotNull(person.firstName, person.lastName)
        .joinToString(" ")
        .takeIf(String::isNotBlank)
        ?: email
        ?: id
    return RelatedUser(
        id = id,
        displayName = displayName,
        email = email,
        avatarUrl = person.avatarUrl
            ?: person.profileImage
            ?: person.image
            ?: person.source,
        subjectUserId = userId ?: id,
        isLinked = isLinked,
        dateOfBirth = person.dateOfBirth ?: person.alternateDateOfBirth,
        gender = person.gender,
        phoneNumber = person.phone
            ?: person.mobile
            ?: person.phoneNumber
            ?: person.mobileNumber,
        firstName = person.firstName,
        address = person.formattedAddress(),
        emergencyContacts = person.emergencyContacts
            .mapIndexedNotNull(::emergencyContactFromDto),
        isGuardian = person.isGuardian?.let { it != 0 },
    )
}

private fun accountFromDto(index: Int, account: AccountDto): Account {
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
): LadderEntry? {
    val entry = resource.fields()
    val teamName = entry.teamName ?: return null
    return LadderEntry(
        position = entry.position ?: index + 1,
        teamId = entry.teamIdentifier,
        teamName = teamName,
        logoUrl = entry.clubLogo,
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

private fun LadderEntryResourceDto.fields(): LadderEntryDto =
    attributes ?: LadderEntryDto(
        position = position,
        teamHashId = teamId,
        teamName = teamName,
    )

private val LadderEntryDto.teamIdentifier: String?
    get() = (teamId ?: teamHashId).textValue

private fun FixtureDto.toDto(
    index: Int,
    competitionName: String? = null,
    venueName: String? = null,
    userTeamId: String? = null,
    status: String? = null,
): Fixture? {
    val resolvedHomeName = homeTeamName ?: homeTeam?.name ?: return null
    val resolvedAwayName = awayTeamName ?: awayTeam?.name ?: return null
    return Fixture(
        id = (id ?: matchId).textValue
            ?: "fixture-$index-$resolvedHomeName-$resolvedAwayName",
        kickoffEpochMillis = (kickoffAt ?: date).epochMillis ?: 0L,
        homeTeamId = (homeTeamId
            ?: homeTeamHashId
            ?: homeTeam?.teamId
            ?: homeTeam?.id).textValue,
        homeTeamName = resolvedHomeName,
        awayTeamId = (awayTeamId
            ?: awayTeamHashId
            ?: awayTeam?.teamId
            ?: awayTeam?.id).textValue,
        awayTeamName = resolvedAwayName,
        competitionName = this.competitionName ?: leagueName ?: competitionName,
        venueName = this.venueName ?: groundName ?: venueName,
        userTeamId = this.userTeamId.textValue ?: userTeamId,
        status = (this.status ?: status).toFixtureStatus(),
        homeScore = homeScore,
        awayScore = awayScore,
        homeTeamLogoUrl = homeTeamLogo ?: homeClubImage,
        awayTeamLogoUrl = awayTeamLogo ?: awayClubImage,
    )
}

private fun MatchResourceDto.toDomain(): Fixture =
    attributes.toDomain(id)

private fun MatchAttributesDto.toDomain(resourceId: JsonPrimitive): Fixture =
    Fixture(
        id = resourceId.textValue
            ?: throw DriblResponseException("Match response contained a blank id"),
        kickoffEpochMillis = date.epochMillis
            ?: throw DriblResponseException("Match response contained an invalid date"),
        homeTeamId = homeTeamId.textValue,
        homeTeamName = homeTeamName,
        awayTeamId = awayTeamId.textValue,
        awayTeamName = awayTeamName,
        competitionName = competitionName ?: leagueName,
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
    )

private fun Fixture.involves(teamId: String): Boolean =
    homeTeamId == teamId || awayTeamId == teamId || userTeamId == teamId

private fun String?.toFixtureStatus(): FixtureStatus =
    when (orEmpty().lowercase()) {
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
