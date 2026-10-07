package com.github.apkelly.drool.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class ResourceList<T>(
    @SerialName("data") val items: List<T> = emptyList(),
)

@Serializable
internal data class NestedResourceResponse<T>(
    @SerialName("data") val data: ResourceList<T>? = null,
)

@Serializable
internal data class SignInResponse(
    @SerialName("status") val status: Int,
    @SerialName("token") val token: String,
    @SerialName("user") val user: PersonDto,
    @SerialName("refresh_token") val refreshToken: String? = null,
)

@Serializable
internal data class ImpersonationResponse(
    @SerialName("token") val token: String? = null,
)

@Serializable
internal data class PersonDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("user_id") val userId: JsonPrimitive? = null,
    @SerialName("account_id") val accountId: JsonPrimitive? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("primary_email") val primaryEmail: String? = null,
    @SerialName("sending_email_address") val sendingEmailAddress: String? = null,
    @SerialName("email_address") val emailAddress: String? = null,
    @SerialName("contact_email") val contactEmail: String? = null,
    @SerialName("value") val value: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("profile_image") val profileImage: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("system_image") val systemImage: String? = null,
    @SerialName("dob") val dateOfBirth: String? = null,
    @SerialName("date_of_birth") val alternateDateOfBirth: String? = null,
    @SerialName("activated") val activated: Boolean? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("self_id") val selfId: JsonPrimitive? = null,
    @SerialName("is_guardian") val isGuardian: Int? = null,
    @SerialName("gender") val gender: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("mobile") val mobile: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("mobile_number") val mobileNumber: String? = null,
    @SerialName("address") val address: AddressDto? = null,
    @SerialName("address_line_1") val addressLine1: String? = null,
    @SerialName("address_line_2") val addressLine2: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("postcode") val postcode: String? = null,
    @SerialName("emergency_contacts")
    val emergencyContacts: List<EmergencyContactDto> = emptyList(),
)

@Serializable
internal data class PersonResourceDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("attributes") val attributes: PersonDto = PersonDto(),
)

@Serializable
internal data class AddressDto(
    @SerialName("line_1") val line1: String? = null,
    @SerialName("line_2") val line2: String? = null,
    @SerialName("suburb") val suburb: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("postcode") val postcode: String? = null,
)

@Serializable
internal data class AccountDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("account_id") val accountId: JsonPrimitive? = null,
    @SerialName("account_name") val accountName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("value") val value: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("attributes") val attributes: AccountAttributesDto? = null,
)

@Serializable
internal data class AccountAttributesDto(
    @SerialName("name") val name: String? = null,
    @SerialName("value") val value: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("organisation_name") val organisationName: String? = null,
    @SerialName("activated") val activated: Boolean? = null,
)

@Serializable
internal data class ProfileResponse(
    @SerialName("data") val data: ProfilePayload? = null,
    @SerialName("profile") val profile: PersonDto? = null,
)

@Serializable
internal data class ProfilePayload(
    @SerialName("account") val account: PersonDto? = null,
    @SerialName("profile") val profile: PersonDto? = null,
    @SerialName("user") val user: PersonDto? = null,
)

@Serializable
internal data class TeamReferenceDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("team_id") val teamId: JsonPrimitive? = null,
    @SerialName("teamId") val teamIdCamel: JsonPrimitive? = null,
    @SerialName("name") val name: String? = null,
)

@Serializable
internal data class ClubDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("club_id") val clubId: JsonPrimitive? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("club_name") val clubName: String? = null,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("primary_color") val primaryColor: String? = null,
    @SerialName("secondary_color") val secondaryColor: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("accent") val accent: String? = null,
)

@Serializable
internal data class TeamDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("team_id") val teamId: JsonPrimitive? = null,
    @SerialName("club_id") val clubId: JsonPrimitive? = null,
    @SerialName("club_name") val clubName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("parsed_name") val parsedName: String? = null,
    @SerialName("team_name") val teamName: String? = null,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("age_group") val ageGroup: String? = null,
    @SerialName("competition_name") val competitionName: String? = null,
    @SerialName("team_competition_name") val teamCompetitionName: String? = null,
    @SerialName("active") val active: Boolean? = null,
    @SerialName("primary_color") val primaryColor: String? = null,
    @SerialName("secondary_color") val secondaryColor: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("accent") val accent: String? = null,
    @SerialName("roles") val roles: List<RoleResourceDto> = emptyList(),
)

@Serializable
internal data class ClubListResponse(
    @SerialName("data") val data: List<ClubResourceDto>,
    @SerialName("meta") val meta: PaginationMetaDto? = null,
)

@Serializable
internal data class TeamListResponse(
    @SerialName("data") val data: List<TeamResourceDto>,
    @SerialName("meta") val meta: PaginationMetaDto? = null,
)

@Serializable
internal data class ClubResourceDto(
    @SerialName("id") val id: JsonPrimitive,
    @SerialName("attributes") val attributes: ClubDto,
)

@Serializable
internal data class PaginationMetaDto(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
)

@Serializable
internal data class MatchListResponse(
    @SerialName("data") val data: List<MatchResourceDto>,
)

@Serializable
internal data class MatchResponse(
    @SerialName("data") val data: MatchResourceDto,
)

@Serializable
internal data class MatchResourceDto(
    @SerialName("id") val id: JsonPrimitive,
    @SerialName("attributes") val attributes: MatchAttributesDto,
)

@Serializable
internal data class MatchAttributesDto(
    @SerialName("match_id") val matchId: JsonPrimitive? = null,
    @SerialName("date") val date: JsonPrimitive,
    @SerialName("home_team_id") val homeTeamId: JsonPrimitive,
    @SerialName("home_team_name") val homeTeamName: String,
    @SerialName("away_team_id") val awayTeamId: JsonPrimitive,
    @SerialName("away_team_name") val awayTeamName: String,
    @SerialName("competition_name") val competitionName: String? = null,
    @SerialName("league_name") val leagueName: String? = null,
    @SerialName("field_name") val fieldName: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("latitude") val latitude: JsonPrimitive? = null,
    @SerialName("longitude") val longitude: JsonPrimitive? = null,
    @SerialName("round_label") val roundLabel: String? = null,
    @SerialName("round_number") val roundNumber: JsonPrimitive? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("home_team_score") val homeTeamScore: Int? = null,
    @SerialName("away_team_score") val awayTeamScore: Int? = null,
    @SerialName("home_team_logo") val homeTeamLogo: String? = null,
    @SerialName("away_team_logo") val awayTeamLogo: String? = null,
    @SerialName("roles") val roles: List<MatchRoleResourceDto> = emptyList(),
)

@Serializable
internal data class MatchRoleResourceDto(
    @SerialName("attributes") val attributes: MatchRoleAttributesDto,
)

@Serializable
internal data class MatchRoleAttributesDto(
    @SerialName("name") val name: String? = null,
    @SerialName("slug") val slug: String? = null,
)

@Serializable
internal data class ScheduleResponse(
    @SerialName("allocations") val allocations: List<ScheduleAllocationResourceDto> = emptyList(),
)

@Serializable
internal data class ScheduleAllocationResourceDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("attributes") val attributes: ScheduleAllocationAttributesDto,
)

@Serializable
internal data class ScheduleAllocationAttributesDto(
    @SerialName("event_id") val eventId: JsonPrimitive? = null,
    @SerialName("date") val date: JsonPrimitive,
    @SerialName("home_team_id") val homeTeamId: JsonPrimitive? = null,
    @SerialName("home_club_name") val homeClubName: String? = null,
    @SerialName("home_team_name") val homeTeamName: String? = null,
    @SerialName("away_team_id") val awayTeamId: JsonPrimitive? = null,
    @SerialName("away_club_name") val awayClubName: String? = null,
    @SerialName("away_team_name") val awayTeamName: String? = null,
    @SerialName("home_club_image") val homeClubImage: String? = null,
    @SerialName("away_club_image") val awayClubImage: String? = null,
    @SerialName("competition_name") val competitionName: String? = null,
    @SerialName("league_name") val leagueName: String? = null,
    @SerialName("ground") val ground: String? = null,
    @SerialName("field") val field: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("referee_role") val refereeRole: String? = null,
    @SerialName("referee_status") val refereeStatus: String? = null,
)

@Serializable
internal data class FixtureDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("match_id") val matchId: JsonPrimitive? = null,
    @SerialName("kickoff_at") val kickoffAt: JsonPrimitive? = null,
    @SerialName("date") val date: JsonPrimitive? = null,
    @SerialName("home_team") val homeTeam: TeamReferenceDto? = null,
    @SerialName("home_team_id") val homeTeamId: JsonPrimitive? = null,
    @SerialName("home_team_hash_id") val homeTeamHashId: JsonPrimitive? = null,
    @SerialName("home_team_name") val homeTeamName: String? = null,
    @SerialName("away_team") val awayTeam: TeamReferenceDto? = null,
    @SerialName("away_team_id") val awayTeamId: JsonPrimitive? = null,
    @SerialName("away_team_hash_id") val awayTeamHashId: JsonPrimitive? = null,
    @SerialName("away_team_name") val awayTeamName: String? = null,
    @SerialName("competition_name") val competitionName: String? = null,
    @SerialName("league_name") val leagueName: String? = null,
    @SerialName("venue_name") val venueName: String? = null,
    @SerialName("ground_name") val groundName: String? = null,
    @SerialName("user_team_id") val userTeamId: JsonPrimitive? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("home_score") val homeScore: Int? = null,
    @SerialName("away_score") val awayScore: Int? = null,
    @SerialName("home_team_logo") val homeTeamLogo: String? = null,
    @SerialName("home_club_image") val homeClubImage: String? = null,
    @SerialName("away_team_logo") val awayTeamLogo: String? = null,
    @SerialName("away_club_image") val awayClubImage: String? = null,
)

@Serializable
internal data class LadderListResponse(
    @SerialName("data") val data: List<LadderSummaryDto> = emptyList(),
    @SerialName("ladders") val ladders: List<LadderSummaryDto> = emptyList(),
)

@Serializable
internal data class LadderSummaryDto(
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("name") val name: String? = null,
)

@Serializable
internal data class LadderDetailResponse(
    @SerialName("ladder_entries") val ladderEntries: List<LadderEntryResourceDto> = emptyList(),
)

@Serializable
internal data class LadderEntryResourceDto(
    @SerialName("attributes") val attributes: LadderEntryDto? = null,
    @SerialName("position") val position: Int? = null,
    @SerialName("team_hash_id") val teamId: JsonPrimitive? = null,
    @SerialName("team_name") val teamName: String? = null,
)

@Serializable
internal data class LadderEntryDto(
    @SerialName("position") val position: Int? = null,
    @SerialName("team_id") val teamId: JsonPrimitive? = null,
    @SerialName("team_hash_id") val teamHashId: JsonPrimitive? = null,
    @SerialName("team_name") val teamName: String? = null,
    @SerialName("league_name") val leagueName: String? = null,
    @SerialName("club_logo") val clubLogo: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("played") val played: Int? = null,
    @SerialName("won") val won: Int? = null,
    @SerialName("drawn") val drawn: Int? = null,
    @SerialName("lost") val lost: Int? = null,
    @SerialName("goals_for") val goalsFor: Int? = null,
    @SerialName("goals_against") val goalsAgainst: Int? = null,
    @SerialName("goal_difference") val goalDifference: Int? = null,
    @SerialName("points") val points: Int? = null,
    @SerialName("upcoming_matches") val upcomingMatches: List<FixtureDto> = emptyList(),
    @SerialName("recent_matches") val recentMatches: List<FixtureDto> = emptyList(),
)

@Serializable
internal data class ShortcutResponse(
    @SerialName("teams") val teams: List<TeamResourceDto> = emptyList(),
)

@Serializable
internal data class TeamResourceDto(
    @SerialName("id") val id: JsonPrimitive,
    @SerialName("attributes") val attributes: TeamDto,
    @SerialName("roles") val roles: List<RoleResourceDto> = emptyList(),
)

@Serializable
internal data class RoleDto(
    @SerialName("slug") val slug: String? = null,
    @SerialName("name") val name: String? = null,
)

@Serializable
internal data class RoleResourceDto(
    @SerialName("attributes") val attributes: RoleDto = RoleDto(),
)

@Serializable
internal data class MemberCardsResponse(
    @SerialName("teams") val teams: List<TeamResourceDto> = emptyList(),
)

@Serializable
internal data class UserResponse(
    @SerialName("data") val data: PersonResourceDto? = null,
)

@Serializable
internal data class EmergencyContactDto(
    @SerialName("name") val name: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("contact_name") val contactName: String? = null,
    @SerialName("phone_number_1") val phoneNumber1: String? = null,
    @SerialName("phone_number_2") val phoneNumber2: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("mobile") val mobile: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("contact_email") val contactEmail: String? = null,
)

@Serializable
internal data class EmergencyContactResourceDto(
    @SerialName("attributes") val attributes: EmergencyContactDto = EmergencyContactDto(),
)

@Serializable
internal data class TokenVerificationRequest(
    @SerialName("token") val token: String,
)
