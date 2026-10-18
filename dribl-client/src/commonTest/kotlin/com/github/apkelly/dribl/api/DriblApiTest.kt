package com.github.apkelly.dribl.api

import com.github.apkelly.dribl.api.driblJson
import com.github.apkelly.dribl.dto.AuthApiRequest
import com.github.apkelly.dribl.dto.CreateRefereeAvailabilityRequest
import com.github.apkelly.dribl.model.AvailabilityPeriod
import com.github.apkelly.dribl.model.EmergencyContact
import com.github.apkelly.dribl.model.Fixture
import com.github.apkelly.dribl.model.FixtureRole
import com.github.apkelly.dribl.model.FixtureStatus
import com.github.apkelly.dribl.model.TeamSheetAction
import com.github.apkelly.dribl.model.TeamSheetStatus
import com.github.apkelly.dribl.model.normalizedTeamName
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Instant

class DriblApiTest {
    @Test
    fun publicClubsDecodeJsonApiResourcesAcrossEveryPage() = runTest {
        val pages = mutableListOf<String?>()
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler { request ->
                    pages += request.url.parameters["page"]
                    assertEquals("1000", request.url.parameters["per_page"])
                    val page = request.url.parameters["page"]
                    respond(
                        content = """
                            {
                              "data": [{
                                "type": "clubs",
                                "id": ${if (page == "1") "\"club-a\"" else "\"club-b\""},
                                "attributes": {
                                  "name": ${if (page == "1") "\"Alpha\"" else "\"Marrickville\""},
                                  "code": ${if (page == "1") "\"ALP\"" else "\"MAR\""}
                                }
                              }],
                              "meta": {
                                "current_page": ${page ?: "1"},
                                "last_page": 2,
                                "per_page": 1000,
                                "total": 2
                              }
                            }
                        """.trimIndent(),
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }

        val clubs = DriblSportsApi(client, "https://example.test/api").fetchClubs("token")

        assertEquals(listOf<String?>("1", "2"), pages)
        assertEquals(listOf("club-a", "club-b"), clubs.map { it.id })
        assertEquals("MAR", clubs.last().shortName)
    }

    @Test
    fun publicTeamsRemainClubScopedWhileDecodingEveryPage() = runTest {
        val pages = mutableListOf<String?>()
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler { request ->
                    pages += request.url.parameters["page"]
                    assertEquals("marrickville", request.url.parameters["club_id"])
                    assertEquals("true", request.url.parameters["current_season"])
                    assertEquals("1000", request.url.parameters["per_page"])
                    val page = request.url.parameters["page"]
                    respond(
                        content = """
                            {
                              "data": [{
                                "type": "teams",
                                "id": ${if (page == "1") "\"team-u12\"" else "\"team-u13\""},
                                "attributes": {
                                  "club_id": "marrickville",
                                  "name": ${if (page == "1") "\"Red Devils\"" else "\"Marrickville Under 13 Mixed Cobh RAMBLERS\""},
                                  "age_group": ${if (page == "1") "\"Under 12\"" else "\"Under 13\""},
                                  "competition_name": ${if (page == "1") "\"Under 12 Mixed Red\"" else "\"Marrickville Under 13 Mixed\""}
                                }
                              }],
                              "meta": {
                                "current_page": ${page ?: "1"},
                                "last_page": 2,
                                "per_page": 1000,
                                "total": 2
                              }
                            }
                        """.trimIndent(),
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }

        val teams = DriblSportsApi(client, "https://example.test/api")
            .fetchTeams("token", "marrickville")

        assertEquals(listOf<String?>("1", "2"), pages)
        assertEquals(listOf("team-u12", "team-u13"), teams.map { it.id })
        assertEquals(listOf("Red Devils", "Cobh RAMBLERS"), teams.map { it.name })
        assertEquals("Under 13", teams.last().ageGroup)
        assertEquals(setOf("marrickville"), teams.map { it.clubId }.toSet())
    }

    @Test
    fun teamNameNormalizationOnlyRemovesACompleteCompetitionPrefix() {
        assertEquals(
            "Sheffield Wednesday",
            normalizedTeamName(
                "Marrickville  Under 13 Mixed - Sheffield Wednesday",
                "marrickville under 13 mixed",
            ),
        )
        assertEquals(
            "Cobh RAMBLERS",
            normalizedTeamName(
                "Marrickville Under 13 Mixed: Cobh RAMBLERS",
                "Marrickville Under 13 Mixed",
            ),
        )
        assertEquals(
            "Marrickville Under 13 Mixedwood",
            normalizedTeamName(
                "Marrickville Under 13 Mixedwood",
                "Marrickville Under 13 Mixed",
            ),
        )
        assertEquals(
            "Marrickville Under 13 Mixed",
            normalizedTeamName(
                "Marrickville Under 13 Mixed",
                "Marrickville Under 13 Mixed",
            ),
        )
        assertEquals(
            "Sheffield Wednesday",
            normalizedTeamName(
                name = "Marrickville Under 13 Mixed  Sheffield Wednesday",
                competitionName = "Under 13 Mixed White Mixed",
                allowEmbeddedLeaguePrefix = true,
            ),
        )
    }

    @Test
    fun fixturesSupportSecondsMillisecondsAndIsoTimestamps() = runTest {
        val api = DriblSportsApi(
            client = jsonClient(
                """
                {
                  "data": [
                    {
                      "id": "seconds",
                      "attributes": {
                        "date": 1234567890,
                        "home_team_id": "home",
                        "home_team_name": "Home",
                        "away_team_id": "away",
                        "away_team_name": "Away"
                      }
                    },
                    {
                      "id": "milliseconds",
                      "attributes": {
                        "date": 1791003216000,
                        "home_team_id": "home",
                        "home_team_name": "Home",
                        "away_team_id": "away",
                        "away_team_name": "Away"
                      }
                    },
                    {
                      "id": "iso",
                      "attributes": {
                        "date": "2026-10-03T04:53:36Z",
                        "home_team_id": "home",
                        "home_team_name": "Home",
                        "away_team_id": "away",
                        "away_team_name": "Away"
                      }
                    }
                  ]
                }
                """.trimIndent(),
                expectedMethod = HttpMethod.Get,
            ),
        )

        val response = api.fetchFixtures("token")
        val fixtures = response

        assertEquals(1_234_567_890_000, fixtures[0].kickoffEpochMillis)
        assertEquals(1_791_003_216_000, fixtures[1].kickoffEpochMillis)
        assertEquals(1_791_003_216_000, fixtures[2].kickoffEpochMillis)
    }

    @Test
    fun fixturesUseExplicitMatchRolesForRefereeAndCoachAppointments() = runTest {
        val api = DriblSportsApi(
            client = jsonClient(
                """
                {
                  "data": [
                    {
                      "id": "referee-match",
                      "attributes": {
                        "date": "2026-10-06T07:00:00Z",
                        "home_team_id": "home",
                        "home_team_name": "Home",
                        "away_team_id": "away",
                        "away_team_name": "Away",
                        "roles": [{
                          "type": "roles",
                          "id": 19,
                          "attributes": {
                            "name": "Referee",
                            "slug": "clubreferee"
                          }
                        }]
                      }
                    },
                    {
                      "id": "coach-match",
                      "attributes": {
                        "date": "2026-10-06T07:30:00Z",
                        "home_team_id": "coached-team",
                        "home_team_name": "Coached Team",
                        "away_team_id": "opponent",
                        "away_team_name": "Opponent",
                        "roles": [{
                          "type": "roles",
                          "id": 20,
                          "attributes": {
                            "name": "Coach",
                            "slug": "teamcoach"
                          }
                        }]
                      }
                    },
                    {
                      "id": "player-match",
                      "attributes": {
                        "date": "2026-10-06T08:00:00Z",
                        "home_team_id": "playing-team",
                        "home_team_name": "Player Team",
                        "away_team_id": "opponent",
                        "away_team_name": "Opponent",
                        "roles": []
                      }
                    }
                  ]
                }
                """.trimIndent(),
                expectedMethod = HttpMethod.Get,
            ),
        )

        val fixtures = api.fetchFixtures("token").associateBy { it.id }

        assertEquals(FixtureRole.Referee, fixtures.getValue("referee-match").role)
        assertEquals(FixtureRole.Coach, fixtures.getValue("coach-match").role)
        assertEquals(FixtureRole.Player, fixtures.getValue("player-match").role)
    }

    @Test
    fun profileParsesNestedAccountWithoutInferringTeamRoles() = runTest {
        val api = DriblSportsApi(
            client = jsonClient(
                """
                {
                  "data": {
                    "account": {
                      "account_id": "account",
                      "display_name": "Alex",
                      "first_name": "Taylor",
                      "email": "alex@example.com",
                      "profile_image": "https://example.test/taylor.png"
                    },
                    "activeTeams": [
                      { "team_id": "team-a" },
                      { "teamId": "team-b" }
                    ]
                  }
                }
                """.trimIndent(),
                expectedMethod = HttpMethod.Get,
            ),
        )

        val profile = api.fetchProfile("token")

        assertEquals("account", profile.accountId)
        assertEquals("Taylor", profile.displayName)
        assertEquals("https://example.test/taylor.png", profile.avatarUrl)
    }

    @Test
    fun malformedProfileIsReportedExplicitly() = runTest {
        val api = DriblSportsApi(
            jsonClient(
                """{"profile":{"name":"Unknown"}}""",
                expectedMethod = HttpMethod.Get,
            ),
        )

        assertFailsWith<DriblResponseException> {
            api.fetchProfile("token")
        }
    }

    @Test
    fun httpFailuresRetainStatusCode() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { respondError(HttpStatusCode.Forbidden, "forbidden") }
            }
        }
        val error = assertFailsWith<DriblHttpException> {
            DriblSportsApi(client).fetchClubs("token")
        }

        assertEquals(403, error.statusCode)
    }

    @Test
    fun signInDecodesObservedTopLevelTokenAndUser() = runTest {
        val api = DriblApi(
            jsonClient(
                """
                {
                  "status": 200,
                  "token": "secret",
                  "user": {
                    "id": 1000,
                    "sending_email_address": "alex@example.com",
                    "primary_email": "alex@example.com",
                    "primary_email_id": 3000,
                    "last_login": "2026-10-05T04:12:25.144002Z",
                    "first_name": "Alex",
                    "last_name": "Player",
                    "is_guest": 0,
                    "image": null,
                    "system_image": "https://example.test/alex.png"
                  },
                  "refresh_token": null
                }
                """.trimIndent(),
                expectedMethod = HttpMethod.Post,
            ),
        )

        val session = api.signIn(
            AuthApiRequest(
                baseUrl = "https://example.test",
                path = "/signin",
                usernameField = "email",
                passwordField = "password",
                username = "alex@example.com",
                password = "password",
            ),
        )

        assertEquals("secret", session.bearerToken)
        assertEquals("1000", session.identity?.id)
        assertEquals("Alex", session.identity?.firstName)
        assertEquals("Player", session.identity?.lastName)
        assertEquals("alex@example.com", session.identity?.email)
        assertEquals("https://example.test/alex.png", session.identity?.avatarUrl)
        assertEquals(emptyList(), session.relatedUsers)
        assertEquals(emptyList(), session.accounts)
    }

    @Test
    fun profileAssociationsUseDedicatedAuthenticatedGetEndpoints() = runTest {
        val requestedPaths = mutableListOf<String>()
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(driblJson)
            }
            engine {
                addHandler { request ->
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("Bearer token", request.headers[HttpHeaders.Authorization])
                    requestedPaths += request.url.encodedPath
                    val body = when (request.url.encodedPath) {
                        "/api/auth/related-users" -> {
                            assertEquals(
                                "alex@example.com",
                                request.url.parameters["email"],
                            )
                            """
                            {
                              "data": [{
                                "id": "related-1",
                                "first_name": "Sam",
                                "last_name": "Player",
                                "value": "sam@example.com",
                                "source": "https://example.test/sam.png",
                                "dob": "2012-03-04",
                                "activated": true,
                                "status": "active"
                              }]
                            }
                            """.trimIndent()
                        }

                        "/api/access/accounts" ->
                            """
                            {
                              "data": [{
                                "id": "account-1",
                                "type": "accounts",
                                "attributes": {
                                  "type": "mobile number",
                                  "value": "0400 000 000",
                                  "activated": true
                                }
                              }]
                            }
                            """.trimIndent()

                        "/api/linked-users" ->
                            """
                            {
                              "data": [
                                {
                                  "id": 313262,
                                  "type": "linked_users",
                                  "attributes": {
                                    "status": "approved",
                                    "self_id": 1000,
                                    "user_id": 1001,
                                    "first_name": "Sam",
                                    "last_name": "Player",
                                    "is_guardian": 1,
                                    "image": "https://example.test/linked-sam.png"
                                  }
                                },
                                {
                                  "id": 9,
                                  "attributes": {
                                    "status": "pending",
                                    "user_id": 99,
                                    "first_name": "Pending"
                                  }
                                }
                              ]
                            }
                            """.trimIndent()

                        else -> error("Unexpected request path ${request.url.encodedPath}")
                    }
                    respond(
                        content = body,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
        val api = DriblSportsApi(client, "https://example.test/api")

        val relatedUsers = api.fetchRelatedUsers("token", "alex@example.com")
        val accounts = api.fetchAccounts("token")
        val linkedUsers = api.fetchLinkedUsers("token")

        assertEquals(
            listOf(
                "/api/auth/related-users",
                "/api/access/accounts",
                "/api/linked-users",
            ),
            requestedPaths,
        )
        assertEquals("Sam Player", relatedUsers.single().displayName)
        assertEquals("sam@example.com", relatedUsers.single().email)
        assertEquals("2012-03-04", relatedUsers.single().dateOfBirth)
        assertEquals(
            "https://example.test/sam.png",
            relatedUsers.single().avatarUrl,
        )
        assertEquals(
            "https://example.test/linked-sam.png",
            linkedUsers.single().avatarUrl,
        )
        assertEquals("1001", linkedUsers.single().id)
        assertEquals("1001", linkedUsers.single().subjectUserId)
        assertEquals("313262", linkedUsers.single().linkId)
        assertEquals(true, linkedUsers.single().isLinked)
        assertEquals("0400 000 000", accounts.single().name)
        assertEquals("mobile number", accounts.single().subtitle)
        assertEquals("Sam Player", linkedUsers.single().displayName)
    }

    @Test
    fun profileSessionAndShortcutUseRecoveredContracts() = runTest {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler { request ->
                    requests += "${request.method.value} ${request.url.encodedPath}?${request.url.encodedQuery}"
                    val body = when (request.url.encodedPath) {
                        "/api/auth/impersonate/1001" ->
                            """{"status":200,"token":"child-token"}"""

                        "/api/access/shortcut" ->
                            """
                            {
                              "teams": [
                                {
                                  "id": "followed",
                                  "attributes": {
                                    "name": "Followers",
                                    "roles": [{
                                      "attributes": {"slug":"teamsupporter"}
                                    }]
                                  }
                                },
                                {
                                  "id": "playing",
                                  "attributes": {
                                    "name": "Players",
                                    "roles": [{
                                      "attributes": {"slug":"player"}
                                    }]
                                  }
                                },
                                {
                                  "id": "coaching",
                                  "attributes": {
                                    "name": "Coaches"
                                  },
                                  "roles": [{
                                    "attributes": {"name":"Coach"}
                                  }]
                                },
                                {
                                  "id": "associated",
                                  "attributes": {
                                    "name": "Associated",
                                    "roles": [{
                                      "attributes": {"slug":"teamofficial"}
                                    }]
                                  }
                                },
                                {
                                  "id": "managing",
                                  "attributes": {
                                    "name": "Managers",
                                    "roles": [{
                                      "attributes": {"slug":"teammanager"}
                                    }]
                                  }
                                }
                              ]
                            }
                            """.trimIndent()

                        "/api/universal/member-cards" ->
                            """
                            {
                              "teams": [{
                                "id": "member-card",
                                "attributes": {
                                  "club_id": "club",
                                  "club_name": "United",
                                  "image": "club.png"
                                }
                              }]
                            }
                            """.trimIndent()

                        else -> error("Unexpected request ${request.url}")
                    }
                    respond(
                        content = body,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
        val api = DriblSportsApi(client, "https://example.test/api")

        assertEquals("child-token", api.createProfileSession("root-token", "1001"))
        val teams = api.fetchProfileTeams("child-token")
        val clubs = api.fetchProfileClubs("child-token")

        assertEquals("followed", teams[0].team.id)
        assertEquals(
            com.github.apkelly.dribl.model.TeamRelationship.Following,
            teams[0].relationship,
        )
        assertEquals("playing", teams[1].team.id)
        assertEquals(
            com.github.apkelly.dribl.model.TeamRelationship.Player,
            teams[1].relationship,
        )
        assertEquals("coaching", teams[2].team.id)
        assertEquals(
            com.github.apkelly.dribl.model.TeamRelationship.Coaching,
            teams[2].relationship,
        )
        assertEquals("associated", teams[3].team.id)
        assertEquals(
            com.github.apkelly.dribl.model.TeamRelationship.None,
            teams[3].relationship,
        )
        assertEquals("managing", teams[4].team.id)
        assertEquals(
            com.github.apkelly.dribl.model.TeamRelationship.Managing,
            teams[4].relationship,
        )
        assertEquals("club", clubs.single().id)
        assertEquals("United", clubs.single().name)
        assertEquals("POST /api/auth/impersonate/1001?", requests[0])
        assertEquals(
            "GET /api/access/shortcut?status=active&require_color=true",
            requests[1],
        )
        assertEquals("GET /api/universal/member-cards?", requests[2])
    }

    @Test
    fun personalInformationAndContactsUseRecoveredContracts() = runTest {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler { request ->
                    requests += "${request.method.value} ${request.url.encodedPath}"
                    val body = when (request.url.encodedPath) {
                        "/api/users/1001" ->
                            """
                            {
                              "data": {
                                "id": 1001,
                                "type": "users",
                                "attributes": {
                                  "first_name": "Jude",
                                  "last_name": "Bellingham",
                                  "gender": "male",
                                  "dob": "2012-03-04",
                                  "mobile": "0400 000 000",
                                  "contact_email": "jude.bellingham@example.com",
                                  "address_line_1": "1 Example Street",
                                  "address_line_2": "Unit 2",
                                  "city": "Sydney",
                                  "state": "NSW",
                                  "postcode": "2000",
                                  "image": "https://example.test/jude.png"
                                }
                              }
                            }
                            """.trimIndent()

                        "/api/user-contacts/" ->
                            """
                            {
                              "data": [
                                {
                                  "type": "user_contacts",
                                  "id": 1,
                                  "attributes": {
                                    "name": "Harry Kane",
                                    "email": "harry.kane@example.com",
                                    "phone_number_1": "0400 111 111",
                                    "phone_number_2": "0400 222 222"
                                  }
                                },
                                {
                                  "type": "user_contacts",
                                  "id": 2,
                                  "attributes": {
                                    "name": "Player"
                                  }
                                }
                              ]
                            }
                            """.trimIndent()

                        else -> error("Unexpected request ${request.url}")
                    }
                    respond(
                        content = body,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
        val api = DriblSportsApi(client, "https://example.test/api")

        val personal = api.fetchPersonalInformation("child-token", "1001")
        val contacts = api.fetchEmergencyContacts("child-token")

        assertEquals("Jude Bellingham", personal.displayName)
        assertEquals("male", personal.gender)
        assertEquals("2012-03-04", personal.dateOfBirth)
        assertEquals("0400 000 000", personal.phoneNumber)
        assertEquals("jude.bellingham@example.com", personal.email)
        assertEquals(
            "1 Example Street, Unit 2, Sydney, NSW, 2000",
            personal.address,
        )
        assertEquals("https://example.test/jude.png", personal.avatarUrl)
        assertEquals(
            listOf(
                EmergencyContact(
                    name = "Harry Kane",
                    phoneNumber = "0400 111 111",
                    email = "harry.kane@example.com",
                ),
            ),
            contacts,
        )
        assertEquals(
            listOf("GET /api/users/1001", "GET /api/user-contacts/"),
            requests,
        )
    }

    @Test
    fun teamHubAndMatchDetailsUseRecoveredContracts() = runTest {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler { request ->
                    requests += "${request.url.encodedPath}?${request.url.encodedQuery}"
                    val body = when (request.url.encodedPath) {
                        "/api/universal/matches" ->
                            """
                            {"data":[
                              {"type":"matches","id":"match","attributes":{
                               "date":"2026-10-10T02:00:00Z",
                               "home_team_id":"team","home_team_name":"Marrickville Under 13 Mixed  Sheffield Wednesday",
                               "away_team_id":"away","away_team_name":"Marrickville Under 13 Mixed  Cobh RAMBLERS",
                               "league_name":"Under 13 Mixed White Mixed",
                               "status":"complete",
                               "home_team_score":2,"away_team_score":1}},
                              {"type":"matches","id":"other","attributes":{
                               "date":"2026-10-11T02:00:00Z",
                               "home_team_id":"x","home_team_name":"X",
                               "away_team_id":"y","away_team_name":"Y"}}
                            ]}
                            """.trimIndent()

                        "/api/universal/ladders" ->
                            """{"data":[{"id":"ladder-a","name":"Other"},{"id":"ladder-b","name":"Under 13 Mixed White Mixed"}]}"""

                        "/api/universal/ladders/ladder-a" ->
                            """{"ladder_entries":[{"attributes":{"team_hash_id":"other","team_name":"Other","position":1}}]}"""

                        "/api/universal/ladders/ladder-b" ->
                            """
                            {"ladder_entries":[
                              {"attributes":{"team_hash_id":"team",
                               "league_name":"Under 13 Mixed White Mixed",
                               "team_name":"Marrickville Under 13 Mixed  Sheffield Wednesday",
                               "image":"home.png",
                               "position":1,"played":5,"won":4,"drawn":1,"lost":0,
                               "goals_for":12,"goals_against":3,"goal_difference":9,
                               "points":13,"club_logo":"home.png",
                               "upcoming_matches":[
                                 {"id":"form","date":"2026-10-20T02:00:00Z",
                                  "home_team_hash_id":"away","home_team_name":"Marrickville Under 13 Mixed  Cobh RAMBLERS",
                                  "away_team_hash_id":"team","away_team_name":"Marrickville Under 13 Mixed  Sheffield Wednesday"}
                               ]}}
                            ]}
                            """.trimIndent()

                        "/api/universal/matches/match" ->
                            """
                            {"data":{"type":"matches","id":"match","attributes":{
                              "date":"2026-10-10T02:00:00Z",
                              "home_team_id":"team","home_team_name":"Marrickville Under 13 Mixed  Sheffield Wednesday",
                              "away_team_id":"away","away_team_name":"Marrickville Under 13 Mixed  Cobh RAMBLERS",
                              "home_team_score":2,"away_team_score":1,
                              "status":"pending","round_label":"Round 5",
                              "matchsheet_id":"sheet-555","tenant_id":"61","season_id":"1926",
                              "address":"1 Main Street",
                              "latitude":"-33.9000","longitude":"151.1700",
                              "home_team_logo":"home.png","away_team_logo":"away.png",
                              "league_name":"Under 13 Mixed White Mixed","field_name":"Main Field"}}}
                            """.trimIndent()

                        else -> error("Unexpected request ${request.url}")
                    }
                    respond(
                        content = body,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
        val api = DriblSportsApi(client, "https://example.test/api")

        val hub = api.fetchTeamHub(
            "token",
            "team",
            "Under 13 Mixed White Mixed",
            "ladder-b",
        )
        val details = api.fetchMatchDetails("token", "match")

        assertEquals(
            listOf("match", "form"),
            (hub.results + hub.matches).map { it.id },
        )
        assertEquals(2, hub.results.first().homeScore)
        assertEquals("Sheffield Wednesday", hub.results.first().homeTeamName)
        assertEquals("Cobh RAMBLERS", hub.results.first().awayTeamName)
        assertEquals("Under 13 Mixed White Mixed", hub.ladderName)
        assertEquals("team", hub.ladder.single().teamId)
        assertEquals("Sheffield Wednesday", hub.ladder.single().teamName)
        assertEquals("home.png", hub.ladder.single().logoUrl)
        assertEquals(13, hub.ladder.single().points)
        assertEquals("home.png", details.homeTeamLogoUrl)
        assertEquals("away.png", details.awayTeamLogoUrl)
        assertEquals("Sheffield Wednesday", details.homeTeamName)
        assertEquals("Cobh RAMBLERS", details.awayTeamName)
        assertEquals("Main Field", details.venueName)
        assertEquals("1 Main Street", details.venueAddress)
        assertEquals("Round 5", details.roundLabel)
        assertEquals(-33.9, details.latitude)
        assertEquals(151.17, details.longitude)
        assertEquals(FixtureStatus.Pending, details.status)
        assertEquals("sheet-555", details.matchSheetId)
        assertEquals("61", details.tenantId)
        assertEquals("1926", details.seasonId)
        assertEquals(
            setOf(
                "/api/universal/matches" +
                    "?league_id=ladder-b&remove_byes=true&require_adherance=true" +
                    "&sort=%2Bdate%2C%2Bhome_team",
                "/api/universal/ladders?",
                "/api/universal/ladders/ladder-b" +
                    "?require_pools=true&require_form=true&require_adjustments=true" +
                    "&require_championship=true",
                "/api/universal/matches/match" +
                    "?require_season=true&require_adherance=true",
            ),
            requests.toSet(),
        )
        assertFalse(requests.any { it.startsWith("/api/universal/ladders/ladder-a?") })
        assertEquals(
            "/api/universal/ladders/ladder-b" +
                "?require_pools=true&require_form=true&require_adjustments=true" +
                "&require_championship=true",
            requests.first { it.startsWith("/api/universal/ladders/ladder-b?") },
        )
    }

    @Test
    fun matchSheetUsesRecoveredTeamMemberAndOfficialContracts() = runTest {
        val requests = mutableListOf<Pair<String, Map<String, String>>>()
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler { request ->
                    val parameters = request.url.parameters.entries()
                        .associate { it.key to it.value.single() }
                    requests += request.url.encodedPath to parameters
                    val body = when (request.url.encodedPath) {
                        "/api/matchsheet-members" -> {
                            val isHome = parameters["team_id"] == "home"
                            """
                            {"data":[{
                              "id":"${if (isHome) "home-member" else "away-member"}",
                              "attributes":{
                                "user_id":"${if (isHome) "101" else "202"}",
                                "team_id":"${parameters["team_id"]}",
                                "first_name":"${if (isHome) "Alex" else ""}",
                                "last_name":"${if (isHome) "Player" else ""}",
                                "first_name_alias":${if (isHome) "null" else "\"Sam\""},
                                "last_name_alias":${if (isHome) "null" else "\"Coach\""},
                                "image_url":"member.png",
                                "role_name":"${if (isHome) "Player" else "Coach"}",
                                "jersey_number":${if (isHome) "7" else "null"},
                                "available":$isHome
                              }
                            }]}
                            """.trimIndent()
                        }

                        "/api/matchsheet-team-submissions/sheet/home" ->
                            """{"data":{"attributes":{"status":"submitted"}}}"""

                        "/api/matchsheet-team-submissions/sheet/away" ->
                            """{"data":{"attributes":{"status":"confirmed"}}}"""

                        "/api/matchsheet-referees" ->
                            """
                            {"data":[{"id":"referee","attributes":{
                              "user_first_name":"Robin","user_last_name":"Referee",
                              "referee_role":"Centre Referee"
                            }}]}
                            """.trimIndent()

                        "/api/matchsheet-officials" ->
                            """
                            {"data":[{"id":"official","attributes":{
                              "name":"Taylor Official","type":"Sideline Official"
                            }}]}
                            """.trimIndent()

                        else -> error("Unexpected request ${request.url}")
                    }
                    respond(
                        content = body,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
        val fixture = Fixture(
            id = "match",
            kickoffEpochMillis = 0,
            homeTeamId = "home",
            homeTeamName = "Home Team",
            awayTeamId = "away",
            awayTeamName = "Away Team",
            competitionName = null,
            venueName = null,
            userTeamId = "home",
            status = FixtureStatus.Scheduled,
            seasonId = "1926",
            tenantId = "61",
            matchSheetId = "sheet",
        )

        val sheet = DriblSportsApi(client, "https://example.test/api")
            .fetchMatchSheet("token", fixture)

        assertEquals("sheet", sheet.id)
        assertEquals(null, sheet.status)
        assertEquals(TeamSheetStatus.Submitted, sheet.homeTeam.status)
        assertEquals(TeamSheetStatus.Confirmed, sheet.awayTeam.status)
        assertEquals(listOf("home-member", "away-member"), sheet.members.map { it.id })
        assertEquals(
            listOf("Alex Player", "Sam Coach"),
            sheet.members.map {
                "${it.firstName} ${it.lastName}"
            },
        )
        assertEquals("7", sheet.members.first().jerseyNumber)
        assertEquals("Robin Referee", sheet.referees.single().displayName)
        assertEquals("Centre Referee", sheet.referees.single().roleName)
        assertEquals("Taylor Official", sheet.officials.single().displayName)
        assertEquals("Sideline Official", sheet.officials.single().roleName)
        assertEquals(6, requests.size)
        assertFalse(requests.any { it.first.contains("/matchsheets/") })
        assertEquals(
            setOf("home", "away"),
            requests.filter { it.first.endsWith("/matchsheet-members") }
                .map { it.second.getValue("team_id") }
                .toSet(),
        )
        requests.filter {
            it.first.endsWith("/matchsheet-referees") ||
                it.first.endsWith("/matchsheet-officials")
        }.forEach { (_, parameters) ->
            assertEquals("sheet", parameters["matchsheet_id"])
            assertEquals("61", parameters["tenant_id"])
            assertEquals("1926", parameters["season_id"])
        }
        assertEquals(
            "true",
            requests.single { it.first.endsWith("/matchsheet-referees") }
                .second["require_user_id"],
        )
    }

    @Test
    fun matchSheetMutationsUseRecoveredBodiesAndNormalizeStatuses() = runTest {
        val requests = mutableListOf<Triple<HttpMethod, String, String>>()
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler { request ->
                    val body = (request.body as OutgoingContent.ByteArrayContent)
                        .bytes()
                        .decodeToString()
                    requests += Triple(request.method, request.url.encodedPath, body)
                    val response = if (request.method == HttpMethod.Put) {
                        val available = driblJson.parseToJsonElement(body)
                            .jsonObject
                            .getValue("available")
                        """
                        {"data":{"id":"member","attributes":{
                          "team_id":"home","first_name":"Alex","last_name":"Player",
                          "role_name":"Player","available":$available
                        }}}
                        """.trimIndent()
                    } else {
                        val action = driblJson.parseToJsonElement(body)
                            .jsonObject
                            .getValue("type")
                            .jsonPrimitive
                            .content
                        val status = when (action) {
                            "submit" -> "submitted"
                            "confirm" -> "confirmed"
                            else -> "open"
                        }
                        """{"data":{"attributes":{"status":"$status"}}}"""
                    }
                    respond(
                        content = response,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
        val api = DriblSportsApi(client, "https://example.test/api")

        assertEquals(
            true,
            api.updateMatchSheetMemberAvailability("token", "member", true).available,
        )
        assertEquals(
            false,
            api.updateMatchSheetMemberAvailability("token", "member", false).available,
        )
        val statuses = TeamSheetAction.entries.map {
            api.updateTeamSheetStatus("token", "sheet", "home", it)
        }

        assertEquals(
            listOf(
                TeamSheetStatus.Submitted,
                TeamSheetStatus.Open,
                TeamSheetStatus.Confirmed,
                TeamSheetStatus.Open,
            ),
            statuses,
        )
        assertEquals(
            driblJson.parseToJsonElement("""{"available":true}"""),
            driblJson.parseToJsonElement(requests[0].third),
        )
        assertEquals(
            driblJson.parseToJsonElement(
                """
                {
                  "available":false,
                  "starting":false,
                  "playing":false,
                  "is_captain":false,
                  "is_goalkeeper":false
                }
                """.trimIndent(),
            ),
            driblJson.parseToJsonElement(requests[1].third),
        )
        assertEquals(
            listOf("submit", "unsubmit", "confirm", "unconfirm"),
            requests.drop(2).map {
                driblJson.parseToJsonElement(it.third)
                    .jsonObject
                    .getValue("type")
                    .jsonPrimitive
                    .content
            },
        )
        requests.drop(2).forEach { (method, path, body) ->
            assertEquals(HttpMethod.Patch, method)
            assertEquals("/api/matchsheet-team-submissions", path)
            val json = driblJson.parseToJsonElement(body).jsonObject
            assertEquals("sheet", json.getValue("matchsheet_id").jsonPrimitive.content)
            assertEquals("home", json.getValue("team_id").jsonPrimitive.content)
        }
    }

    @Test
    fun matchSheetRejectsBlankRequiredIdentifiersWithoutARequest() = runTest {
        var requested = false
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler {
                    requested = true
                    respond("""{"data":[]}""")
                }
            }
        }
        val valid = Fixture(
            id = "match",
            kickoffEpochMillis = 0,
            homeTeamId = "home",
            homeTeamName = "Home",
            awayTeamId = "away",
            awayTeamName = "Away",
            competitionName = null,
            venueName = null,
            userTeamId = "home",
            status = FixtureStatus.Scheduled,
            matchSheetId = "sheet",
        )
        val api = DriblSportsApi(client, "https://example.test/api")

        assertFailsWith<DriblResponseException> {
            api.fetchMatchSheet("token", valid.copy(matchSheetId = " "))
        }
        assertFailsWith<DriblResponseException> {
            api.fetchMatchSheet("token", valid.copy(homeTeamId = ""))
        }
        assertFailsWith<DriblResponseException> {
            api.fetchMatchSheet("token", valid.copy(awayTeamId = "\t"))
        }
        assertFalse(requested)
    }

    @Test
    fun refereeAllocationsUseUserScopedCollectionAndBoundedWindow() = runTest {
        var parameters: io.ktor.http.Parameters? = null
        var path: String? = null
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler { request ->
                    path = request.url.encodedPath
                    parameters = request.url.parameters
                    respond(
                        content = """
                            {
                              "data": [{
                                "type": "allocations",
                                "id": 123,
                                "attributes": {
                                  "event_id": 456,
                                  "date": "2026-10-18T03:00:00Z",
                                  "home_team_id": 10,
                                  "home_club_name": "Home FC",
                                  "away_team_id": 20,
                                  "away_club_name": "Away FC",
                                  "home_club_image": "home.png",
                                  "away_club_image": "away.png",
                                  "competition_name": "Under 13 Mixed",
                                  "ground": "Main Oval",
                                  "field": "Field 1",
                                  "referee_role": "AR1",
                                  "referee_status": "accepted"
                                }
                              }]
                            }
                        """.trimIndent(),
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }

        val fixture = DriblSportsApi(
            client = client,
            baseUrl = "https://example.test/api",
            currentEpochMillis = { 1_760_788_800_000L },
        )
            .fetchRefereeFixtures("token", "user-42")
            .single()

        assertEquals("/api/universal/allocations/", path)
        assertEquals("456", fixture.id)
        assertEquals("Home FC", fixture.homeTeamName)
        assertEquals("Away FC", fixture.awayTeamName)
        assertEquals("Main Oval - Field 1", fixture.venueName)
        assertEquals(FixtureRole.Referee, fixture.role)
        assertEquals("AR1", fixture.refereeRole)
        assertEquals("123", fixture.refereeAllocationId)
        assertEquals("accepted", fixture.refereeStatus)
        assertEquals("user-42", parameters?.get("user_id"))
        assertEquals("2025-10-04T12:00:00Z", parameters?.get("start_date"))
        assertEquals("2026-01-16T12:00:00Z", parameters?.get("end_date"))
    }

    @Test
    fun pendingRefereeAllocationAcceptsMatchAndStartDateFields() = runTest {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler {
                    respond(
                        content = """
                            {
                              "data": [{
                                "type": "allocations",
                                "id": 789,
                                "attributes": {
                                  "match_id": 654,
                                  "start_date": "2026-11-03T07:00:00Z",
                                  "home_club_name": "Home FC",
                                  "away_club_name": "Away FC",
                                  "referee_status": "pending"
                                }
                              }]
                            }
                        """.trimIndent(),
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }

        val fixture = DriblSportsApi(client, "https://example.test/api")
            .fetchRefereeFixtures("token", "user-42")
            .single()

        assertEquals("654", fixture.id)
        assertEquals(
            Instant.parse("2026-11-03T07:00:00Z").toEpochMilliseconds(),
            fixture.kickoffEpochMillis,
        )
        assertEquals("789", fixture.refereeAllocationId)
        assertEquals("pending", fixture.refereeStatus)
    }

    @Test
    fun refereeAllocationStatusUsesRecoveredPatchContract() = runTest {
        var body: String? = null
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler { request ->
                    assertEquals(HttpMethod.Patch, request.method)
                    assertEquals(
                        "/api/universal/allocations/allocation-123",
                        request.url.encodedPath,
                    )
                    body = (request.body as OutgoingContent.ByteArrayContent)
                        .bytes()
                        .decodeToString()
                    respond(
                        content = """{"data":{"id":"allocation-123"}}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }

        DriblSportsApi(client, "https://example.test/api")
            .updateRefereeAllocation("token", "allocation-123", "accepted")

        assertEquals(
            driblJson.parseToJsonElement("""{"status":"accepted"}"""),
            driblJson.parseToJsonElement(checkNotNull(body)),
        )
    }

    @Test
    fun linkMemberContractsAndPersonalFieldsMatchOriginalClient() = runTest {
        val requests = mutableListOf<Pair<HttpMethod, String>>()
        var linkRequestBody: String? = null
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler { request ->
                    requests += request.method to request.url.encodedPath
                    if (request.method == HttpMethod.Post) {
                        linkRequestBody = (
                            request.body as OutgoingContent.ByteArrayContent
                            ).bytes().decodeToString()
                    }
                    val response = when (request.method) {
                        HttpMethod.Get ->
                            """
                        {
                          "data": [
                            {
                              "first_name": "Jude",
                              "last_name": "Bellingham",
                              "source": "https://ocean.dribl.com/jude",
                              "dob": "2012-03-04",
                              "id": 1001,
                              "gender": "Male",
                              "phone_number": "0400 000 000",
                              "address": {
                                "line_1": "1 Example Street",
                                "suburb": "Sydney",
                                "postcode": "2000"
                              },
                              "emergency_contacts": [{
                                "name": "Harry Kane",
                                "phone": "0400 111 111"
                              }]
                            },
                            {
                              "first_name": "Harry",
                              "last_name": "Kane",
                              "source": "https://ocean.dribl.com/harry",
                              "dob": "1993-07-28",
                              "id": 1000
                            }
                          ]
                        }
                            """.trimIndent()

                        HttpMethod.Post ->
                            """{"data":{"id":"linked-candidate","attributes":{"status":"pending"}}}"""

                        else -> """{"data":{"status":"approved"}}"""
                    }
                    respond(
                        content = response,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
        val api = DriblSportsApi(client, "https://example.test/api")

        val candidates = api.fetchLinkCandidates("token")
        val candidate = candidates.first()
        val linkResult = api.requestLinkedUser("token", candidate.id, true)
        api.verifyLinkedUser("token", linkResult.relationshipId, "123456")
        api.unlinkLinkedUser("token", "linked-candidate")

        assertEquals("Male", candidate.gender)
        assertEquals("1001", candidate.id)
        assertEquals("https://ocean.dribl.com/jude", candidate.avatarUrl)
        assertEquals("1000", candidates.last().id)
        assertEquals("2012-03-04", candidate.dateOfBirth)
        assertEquals("0400 000 000", candidate.phoneNumber)
        assertEquals("1 Example Street, Sydney, 2000", candidate.address)
        assertEquals(
            listOf(EmergencyContact("Harry Kane", "0400 111 111")),
            candidate.emergencyContacts,
        )
        assertEquals("linked-candidate", linkResult.relationshipId)
        assertEquals(false, linkResult.isApproved)
        assertEquals(
            driblJson.parseToJsonElement(
                """{"related_user":{"id":"1001","is_guardian":true},"approve":true}""",
            ),
            driblJson.parseToJsonElement(checkNotNull(linkRequestBody)),
        )
        assertEquals(
            listOf(
                HttpMethod.Get to "/api/linked-users-lookup",
                HttpMethod.Post to "/api/linked-users",
                HttpMethod.Patch to "/api/linked-users/linked-candidate",
                HttpMethod.Delete to "/api/linked-users/linked-candidate",
            ),
            requests,
        )
    }

    @Test
    fun approvedLinkMemberResponseDoesNotRequireVerification() = runTest {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler {
                    respond(
                        content = """
                            {
                              "data": {
                                "id": 2001,
                                "type": "linked_users",
                                "attributes": {
                                  "status": "approved",
                                  "self_id": 1000,
                                  "user_id": 1001,
                                  "first_name": "Jude",
                                  "last_name": "Bellingham",
                                  "is_guardian": 1
                                }
                              }
                            }
                        """.trimIndent(),
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }

        val result = DriblSportsApi(client, "https://example.test/api")
            .requestLinkedUser("token", "1001", true)

        assertEquals("2001", result.relationshipId)
        assertEquals(true, result.isApproved)
    }

    @Test
    fun refereeAvailabilityEndpointsDecodeAndSendTheRecoveredContract() = runTest {
        val requests = mutableListOf<Pair<HttpMethod, String>>()
        var postBody: String? = null
        val availabilityJson = """
            {
              "data": {
                "type": "availabilities",
                "id": 21484775,
                "attributes": {
                  "start": "2026-10-13T07:00:00.000000Z",
                  "end": "2026-10-13T08:00:00.000000Z",
                  "club_id": 5129,
                  "referee_availability_template_period": "Weekly",
                  "referee_availability_template_end": "2026-12-29 08:00:00",
                  "clubs": [{"id": 5129, "name": "Marrickville Referees"}]
                }
              }
            }
        """.trimIndent()
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(driblJson) }
            engine {
                addHandler { request ->
                    requests += request.method to request.url.encodedPath
                    if (request.method == HttpMethod.Get) {
                        assertEquals(
                            "2026-10-11T00:00:00Z",
                            request.url.parameters["start_date"],
                        )
                        assertEquals("asc", request.url.parameters["direction"])
                        assertEquals("1001", request.url.parameters["user_id"])
                    }
                    if (request.method == HttpMethod.Post) {
                        postBody = (
                            request.body as OutgoingContent.ByteArrayContent
                            ).bytes().decodeToString()
                    }
                    respond(
                        content = if (request.method == HttpMethod.Get) {
                            availabilityJson.replace("\"data\": {", "\"data\": [{")
                                .replace("\n  }\n}", "\n  }]\n}")
                        } else {
                            availabilityJson
                        },
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
        val api = DriblSportsApi(client, "https://example.test/api")

        val loaded = api.fetchRefereeAvailabilities(
            bearerToken = "token",
            userId = "1001",
            startDate = "2026-10-11T00:00:00Z",
        ).single()
        val created = api.createRefereeAvailability(
            bearerToken = "token",
            request = CreateRefereeAvailabilityRequest(
                userId = "1001",
                start = "2026-10-13T07:00:00Z",
                end = "2026-12-29T08:00:00Z",
                startTime = "18:00",
                endTime = "19:00",
                allClubs = false,
                clubId = "5129",
                period = "Weekly",
                day = 2,
                timeZone = "Australia/Sydney",
                note = null,
            ),
        )
        api.deleteRefereeAvailability("token", "21484775")

        assertEquals(AvailabilityPeriod.Weekly, loaded.period)
        assertEquals("Marrickville Referees", loaded.clubName)
        assertEquals(21484775.toString(), created.id)
        assertEquals(
            listOf(
                HttpMethod.Get to "/api/universal/availabilities",
                HttpMethod.Post to "/api/universal/availabilities",
                HttpMethod.Delete to "/api/universal/availabilities/21484775",
            ),
            requests,
        )
        val body = driblJson.parseToJsonElement(checkNotNull(postBody)).toString()
        assertEquals(
            """{"user_id":"1001","start":"2026-10-13T07:00:00Z","end":""" +
                """"2026-12-29T08:00:00Z","start_time":"18:00","end_time":"19:00",""" +
                """"all_clubs":false,"club_id":"5129","period":"Weekly","day":2,""" +
                """"use_timezone":"Australia/Sydney"}""",
            body,
        )
    }

    @Test
    fun activeRefereeClubsDecodeTimezone() = runTest {
        val client = jsonClient(
            """
            {
              "data": [{
                "type": "clubs",
                "id": 5129,
                "attributes": {
                  "name": "Marrickville Referees",
                  "logo_url": "https://example.test/logo.png",
                  "settings": {"timezone": "Australia/Sydney"}
                }
              }]
            }
            """.trimIndent(),
        )

        val club = DriblSportsApi(client, "https://example.test/api")
            .fetchActiveRefereeClubs("token")
            .single()

        assertEquals("5129", club.id)
        assertEquals("Marrickville Referees", club.name)
        assertEquals("Australia/Sydney", club.timeZone)
    }

    private fun jsonClient(
        body: String,
        expectedMethod: HttpMethod? = null,
    ): HttpClient = HttpClient(MockEngine) {
        install(ContentNegotiation) {
            json(driblJson)
        }
        engine {
            addHandler { request ->
                expectedMethod?.let { assertEquals(it, request.method) }
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
    }
}
