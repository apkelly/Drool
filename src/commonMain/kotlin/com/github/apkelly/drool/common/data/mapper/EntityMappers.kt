package com.github.apkelly.drool.common.data.mapper

import com.github.apkelly.dribl.model.normalizedTeamName
import com.github.apkelly.drool.common.data.local.entity.AccountEntity
import com.github.apkelly.drool.common.data.local.entity.ClubEntity
import com.github.apkelly.drool.common.data.local.entity.FixtureEntity
import com.github.apkelly.drool.common.data.local.entity.ProfileEntity
import com.github.apkelly.drool.common.data.local.entity.RelatedUserEntity
import com.github.apkelly.drool.common.data.local.entity.TeamEntity
import com.github.apkelly.drool.common.domain.model.Account
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.EmergencyContact
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.FixtureStatus
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.Team
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Club.toEntity() = ClubEntity(id, name, shortName, logoUrl, primaryColor, secondaryColor)
fun ClubEntity.toDomain() = Club(id, name, shortName, logoUrl, primaryColor, secondaryColor)

fun Team.toEntity() = TeamEntity(
    id,
    clubId,
    name,
    shortName,
    logoUrl,
    ageGroup,
    competitionName,
    active,
    primaryColor,
    secondaryColor,
)

fun TeamEntity.toDomain() = Team(
    id,
    clubId,
    normalizedTeamName(name, competitionName),
    shortName,
    logoUrl,
    ageGroup,
    competitionName,
    active,
    primaryColor,
    secondaryColor,
)

fun Fixture.toEntity(
    accountId: String,
) = FixtureEntity(
    id = id,
    accountId = accountId,
    kickoffEpochMillis = kickoffEpochMillis,
    homeTeamId = homeTeamId,
    homeTeamName = normalizedTeamName(
        homeTeamName,
        competitionName,
        allowEmbeddedLeaguePrefix = true,
    ),
    awayTeamId = awayTeamId,
    awayTeamName = normalizedTeamName(
        awayTeamName,
        competitionName,
        allowEmbeddedLeaguePrefix = true,
    ),
    competitionName = competitionName,
    venueName = venueName,
    userTeamId = userTeamId,
    status = status.name,
    role = role.name,
    refereeRole = refereeRole,
    leagueName = leagueName,
    homeScore = homeScore,
    awayScore = awayScore,
    homeTeamLogoUrl = homeTeamLogoUrl,
    awayTeamLogoUrl = awayTeamLogoUrl,
)

fun FixtureEntity.toDomain() = Fixture(
    id = id,
    profileId = accountId,
    kickoffEpochMillis = kickoffEpochMillis,
    homeTeamId = homeTeamId,
    homeTeamName = normalizedTeamName(
        homeTeamName,
        competitionName,
        allowEmbeddedLeaguePrefix = true,
    ),
    awayTeamId = awayTeamId,
    awayTeamName = normalizedTeamName(
        awayTeamName,
        competitionName,
        allowEmbeddedLeaguePrefix = true,
    ),
    competitionName = competitionName,
    venueName = venueName,
    userTeamId = userTeamId,
    status = when (status.lowercase()) {
        "pending" -> FixtureStatus.Pending
        "scheduled", "upcoming", "fixture" -> FixtureStatus.Scheduled
        "live", "in_progress" -> FixtureStatus.Live
        "completed", "complete", "final" -> FixtureStatus.Completed
        "postponed" -> FixtureStatus.Postponed
        "washout" -> FixtureStatus.Washout
        "cancelled", "canceled" -> FixtureStatus.Cancelled
        else -> FixtureStatus.Unknown
    },
    homeScore = homeScore,
    awayScore = awayScore,
    homeTeamLogoUrl = homeTeamLogoUrl,
    awayTeamLogoUrl = awayTeamLogoUrl,
    role = runCatching { FixtureRole.valueOf(role) }.getOrDefault(FixtureRole.Player),
    refereeRole = refereeRole,
    leagueName = leagueName,
)

fun Profile.toEntity() = ProfileEntity(accountId, displayName, email, active = true, avatarUrl = avatarUrl)

fun ProfileEntity.toDomain(
    relatedUsers: List<RelatedUser> = emptyList(),
    accounts: List<Account> = emptyList(),
) = Profile(accountId, displayName, email, avatarUrl, relatedUsers, accounts)

fun ProfileEntity?.toCurrentProfile(
    fallback: Profile,
): Profile = this?.takeIf { it.accountId == fallback.accountId }?.toDomain() ?: fallback

fun RelatedUser.toEntity(
    ownerAccountId: String,
): RelatedUserEntity = RelatedUserEntity(
    ownerAccountId = ownerAccountId,
    id = id,
    displayName = displayName,
    email = email,
    avatarUrl = avatarUrl,
    linkId = linkId,
    gender = gender,
    dateOfBirth = dateOfBirth,
    phoneNumber = phoneNumber,
    address = address,
    emergencyContactsJson = emergencyContacts.encodeForStorage(),
    isLinked = isLinked,
    firstName = firstName,
    isGuardian = isGuardian,
)

fun RelatedUser.toLinkedEntity(
    ownerAccountId: String,
): RelatedUserEntity = toEntity(ownerAccountId).copy(isLinked = true)

/**
 * Combines linked profiles and linked-user records into one stable cached identity list.
 *
 * Dribl exposes these identities through separate endpoints and may assign different IDs before
 * linking. Matching therefore falls back conservatively from ID to unique email to unique
 * normalized name. Linked data wins when present, while richer profile fields fill gaps.
 */
fun reconcileRelatedUsers(
    relatedUsers: List<RelatedUserEntity>,
    linkedUsers: List<RelatedUserEntity>,
): List<RelatedUserEntity> {
    val remaining = relatedUsers.distinctBy { it.id }.toMutableList()
    val reconciled = linkedUsers.distinctBy { it.id }.map { linked ->
        val matched = remaining.firstOrNull { it.id == linked.id }
            ?: linked.email?.takeIf { it.isNotBlank() }?.let { email ->
                remaining.singleOrNull { it.email.equals(email, ignoreCase = true) }
            }
            ?: linked.displayName.normalizedPersonName()
                .takeIf { it.isNotEmpty() && linked.displayName != linked.id }
                ?.let { name ->
                    remaining.singleOrNull {
                        it.displayName.normalizedPersonName() == name
                    }
                }
        if (matched != null) {
            remaining.remove(matched)
            linked.mergedWith(matched)
        } else {
            linked.copy(isLinked = true)
        }
    }
    return remaining.filterNot { it.isLinked } + reconciled
}

private fun RelatedUserEntity.mergedWith(
    matched: RelatedUserEntity,
): RelatedUserEntity = copy(
    displayName = displayName.takeUnless { it == id } ?: matched.displayName,
    email = email ?: matched.email,
    avatarUrl = avatarUrl ?: matched.avatarUrl,
    gender = gender ?: matched.gender,
    dateOfBirth = dateOfBirth ?: matched.dateOfBirth,
    phoneNumber = phoneNumber ?: matched.phoneNumber,
    address = address ?: matched.address,
    emergencyContactsJson = emergencyContactsJson ?: matched.emergencyContactsJson,
    firstName = firstName ?: matched.firstName,
    isGuardian = isGuardian ?: matched.isGuardian,
    isLinked = true,
)

private fun String.normalizedPersonName(): String = trim().lowercase().split(Regex("\\s+")).joinToString(" ")

/**
 * Enriches the cached account owner with identity data returned by linked-profile endpoints.
 *
 * Authentication responses can contain only a placeholder name or omit an avatar, so this keeps
 * the current profile useful without replacing values when no unambiguous identity is available.
 */
fun ProfileEntity.withIdentityFrom(
    users: List<RelatedUser>,
    email: String,
): ProfileEntity {
    val identity = users.firstOrNull { it.id == accountId }
        ?: users.firstOrNull {
            it.displayName.isNotBlank() && it.email.equals(email, ignoreCase = true)
        }
        ?: return this
    return copy(
        displayName = identity.displayName.trim().takeIf { it.isNotEmpty() } ?: displayName,
        email = this.email ?: identity.email,
        avatarUrl = identity.avatarUrl ?: avatarUrl,
    )
}

/**
 * Applies the single non-guardian linked identity to the cached account owner.
 *
 * A unique non-guardian is the only safe signal that the linked-member response describes the
 * signed-in person. Ambiguous responses deliberately leave the existing profile unchanged.
 */
fun ProfileEntity.withLinkedIdentity(
    users: List<RelatedUser>,
): ProfileEntity {
    val identity = users.singleOrNull { it.isGuardian == false } ?: return this
    return copy(
        displayName = identity.firstName
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: displayName,
        avatarUrl = identity.avatarUrl ?: avatarUrl,
    )
}

fun ProfileEntity?.linkedIdentityUpdates(
    accountId: String,
    users: List<RelatedUser>,
): List<ProfileEntity> = this?.takeIf { it.accountId == accountId }
    ?.withLinkedIdentity(users)
    ?.let(::listOf)
    .orEmpty()

fun List<Account>.emailAddress(): String? = firstNotNullOfOrNull { account ->
    account.name.takeIf { it.looksLikeEmailAddress() }
}

fun ProfileEntity.withAccountEmail(
    accounts: List<Account>,
): ProfileEntity = copy(email = email ?: accounts.emailAddress())

fun ProfileEntity?.accountEmailUpdates(
    accountId: String,
    accounts: List<Account>,
): List<ProfileEntity> = this?.takeIf { it.accountId == accountId }
    ?.withAccountEmail(accounts)
    ?.let(::listOf)
    .orEmpty()

private fun String.looksLikeEmailAddress(): Boolean =
    substringBefore('@').isNotBlank() && substringAfter('@', "").contains('.')

fun RelatedUserEntity.toDomain() = RelatedUser(
    id,
    displayName,
    email,
    avatarUrl,
    subjectUserId = id,
    linkId = linkId,
    isLinked = isLinked,
    gender = gender,
    dateOfBirth = dateOfBirth,
    phoneNumber = phoneNumber,
    address = address,
    emergencyContacts = emergencyContactsJson.decodeEmergencyContacts(),
    firstName = firstName,
    isGuardian = isGuardian,
)

/**
 * Produces the emergency-contact JSON persisted for a related user.
 *
 * Contacts arrive from two Dribl endpoints, so valid entries are combined and deduplicated. When
 * neither endpoint supplies usable details, the existing cached contacts are retained to prevent
 * a partial refresh from erasing previously available safety information.
 */
fun mergeEmergencyContacts(
    personalInformationContacts: List<EmergencyContact>,
    contactEndpointContacts: List<EmergencyContact>,
    existingContacts: String?,
): String? {
    val contacts = (personalInformationContacts + contactEndpointContacts)
        .filter(EmergencyContact::hasContactDetails)
        .distinct()
        .ifEmpty { existingContacts.decodeEmergencyContacts() }
    return contacts.encodeForStorage()
}

private val emergencyContactJson = Json {
    ignoreUnknownKeys = true
}

private fun List<EmergencyContact>.encodeForStorage(): String? = takeIf(List<EmergencyContact>::isNotEmpty)
    ?.let(emergencyContactJson::encodeToString)

private fun String?.decodeEmergencyContacts(): List<EmergencyContact> {
    val stored = this?.trim().orEmpty()
    if (stored.isEmpty()) return emptyList()
    return emergencyContactJson.decodeFromString<List<EmergencyContact>>(stored)
        .filter(EmergencyContact::hasContactDetails)
        .distinct()
}

private fun EmergencyContact.hasContactDetails(): Boolean = !phoneNumber.isNullOrBlank() || !email.isNullOrBlank()

fun Account.toEntity(
    ownerAccountId: String,
) = AccountEntity(ownerAccountId, id, name, subtitle, logoUrl)

fun AccountEntity.toDomain() = Account(id, name, subtitle, logoUrl)
