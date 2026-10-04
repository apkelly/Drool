package com.github.apkelly.drool.data.mapper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.github.apkelly.drool.data.local.entity.ClubEntity
import com.github.apkelly.drool.data.local.entity.AccountEntity
import com.github.apkelly.drool.data.local.entity.FixtureEntity
import com.github.apkelly.drool.data.local.entity.ProfileEntity
import com.github.apkelly.drool.data.local.entity.RelatedUserEntity
import com.github.apkelly.drool.data.local.entity.TeamEntity
import com.github.apkelly.drool.domain.model.Club
import com.github.apkelly.drool.domain.model.Account
import com.github.apkelly.drool.domain.model.EmergencyContact
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FixtureStatus
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.RelatedUser
import com.github.apkelly.drool.domain.model.Team
import com.github.apkelly.drool.domain.model.TeamHub
import com.github.apkelly.drool.domain.model.LadderEntry

fun Club.toEntity() = ClubEntity(id, name, shortName, logoUrl, primaryColor, secondaryColor)
fun ClubEntity.toDomain() = Club(id, name, shortName, logoUrl, primaryColor, secondaryColor)

fun Team.toEntity() =
    TeamEntity(
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

fun TeamEntity.toDomain() =
    Team(
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

fun Fixture.toEntity(accountId: String) =
    FixtureEntity(
        id = id,
        accountId = accountId,
        kickoffEpochMillis = kickoffEpochMillis,
        homeTeamId = homeTeamId,
        homeTeamName = homeTeamName,
        awayTeamId = awayTeamId,
        awayTeamName = awayTeamName,
        competitionName = competitionName,
        venueName = venueName,
        userTeamId = userTeamId,
        status = status.name,
    )

fun FixtureEntity.toDomain() =
    Fixture(
        id = id,
        profileId = accountId,
        kickoffEpochMillis = kickoffEpochMillis,
        homeTeamId = homeTeamId,
        homeTeamName = homeTeamName,
        awayTeamId = awayTeamId,
        awayTeamName = awayTeamName,
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
        homeScore = null,
        awayScore = null,
        homeTeamLogoUrl = null,
        awayTeamLogoUrl = null,
    )

fun Profile.toEntity() =
    ProfileEntity(accountId, displayName, email, active = true, avatarUrl = avatarUrl)

fun ProfileEntity.toDomain(
    relatedUsers: List<RelatedUser> = emptyList(),
    accounts: List<Account> = emptyList(),
) = Profile(accountId, displayName, email, avatarUrl, relatedUsers, accounts)

fun ProfileEntity?.toCurrentProfile(fallback: Profile): Profile =
    this?.takeIf { it.accountId == fallback.accountId }?.toDomain() ?: fallback

fun RelatedUser.toEntity(ownerAccountId: String): RelatedUserEntity {
    return RelatedUserEntity(
        ownerAccountId = ownerAccountId,
        id = id,
        displayName = displayName,
        email = email,
        avatarUrl = avatarUrl,
        gender = gender,
        dateOfBirth = dateOfBirth,
        phoneNumber = phoneNumber,
        address = address,
        emergencyContactsJson = emergencyContacts.encodeForStorage(),
        isLinked = isLinked,
    )
}

fun RelatedUser.toLinkedEntity(ownerAccountId: String): RelatedUserEntity =
    toEntity(ownerAccountId).copy(isLinked = true)

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
            linked.copy(
                displayName = linked.displayName.takeUnless { it == linked.id }
                    ?: matched.displayName,
                email = linked.email ?: matched.email,
                avatarUrl = linked.avatarUrl ?: matched.avatarUrl,
                gender = linked.gender ?: matched.gender,
                dateOfBirth = linked.dateOfBirth ?: matched.dateOfBirth,
                phoneNumber = linked.phoneNumber ?: matched.phoneNumber,
                address = linked.address ?: matched.address,
                emergencyContactsJson =
                    linked.emergencyContactsJson ?: matched.emergencyContactsJson,
                isLinked = true,
            )
        } else {
            linked.copy(isLinked = true)
        }
    }
    return remaining.filterNot { it.isLinked } + reconciled
}

private fun String.normalizedPersonName(): String =
    trim().lowercase().split(Regex("\\s+")).joinToString(" ")

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

fun ProfileEntity.withLinkedIdentity(users: List<RelatedUser>): ProfileEntity {
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
): List<ProfileEntity> =
    this?.takeIf { it.accountId == accountId }
        ?.withLinkedIdentity(users)
        ?.let(::listOf)
        .orEmpty()

fun List<Account>.emailAddress(): String? =
    firstNotNullOfOrNull { account ->
        account.name.takeIf { it.looksLikeEmailAddress() }
    }

fun ProfileEntity.withAccountEmail(accounts: List<Account>): ProfileEntity =
    copy(email = email ?: accounts.emailAddress())

fun ProfileEntity?.accountEmailUpdates(
    accountId: String,
    accounts: List<Account>,
): List<ProfileEntity> =
    this?.takeIf { it.accountId == accountId }
        ?.withAccountEmail(accounts)
        ?.let(::listOf)
        .orEmpty()

private fun String.looksLikeEmailAddress(): Boolean =
    substringBefore('@').isNotBlank() && substringAfter('@', "").contains('.')

fun RelatedUserEntity.toDomain() =
    RelatedUser(
        id,
        displayName,
        email,
        avatarUrl,
        subjectUserId = id,
        isLinked = isLinked,
        gender = gender,
        dateOfBirth = dateOfBirth,
        phoneNumber = phoneNumber,
        address = address,
        emergencyContacts = emergencyContactsJson.decodeEmergencyContacts(),
    )

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

private fun List<EmergencyContact>.encodeForStorage(): String? =
    takeIf(List<EmergencyContact>::isNotEmpty)
        ?.let(emergencyContactJson::encodeToString)

private fun String?.decodeEmergencyContacts(): List<EmergencyContact> {
    val stored = this?.trim().orEmpty()
    if (stored.isEmpty()) return emptyList()
    return emergencyContactJson.decodeFromString<List<EmergencyContact>>(stored)
        .filter(EmergencyContact::hasContactDetails)
        .distinct()
}

private fun EmergencyContact.hasContactDetails(): Boolean =
    !phoneNumber.isNullOrBlank() || !email.isNullOrBlank()

fun Account.toEntity(ownerAccountId: String) =
    AccountEntity(ownerAccountId, id, name, subtitle, logoUrl)

fun AccountEntity.toDomain() = Account(id, name, subtitle, logoUrl)
