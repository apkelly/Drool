package com.github.apkelly.drool.domain.model

import kotlinx.serialization.Serializable

data class RelatedUser(
    val id: String,
    val displayName: String,
    val email: String?,
    val avatarUrl: String?,
    val subjectUserId: String = id,
    val isLinked: Boolean = false,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val firstName: String? = null,
    val isGuardian: Boolean? = null,
)

@Serializable
data class EmergencyContact(
    val name: String,
    val phoneNumber: String? = null,
    val email: String? = null,
)

data class FamilyProfile(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val isPrimary: Boolean,
)

fun List<FamilyProfile>.primaryProfileId(): String? =
    firstOrNull(FamilyProfile::isPrimary)?.id

data class Account(
    val id: String,
    val name: String,
    val subtitle: String?,
    val logoUrl: String?,
)

enum class LinkMemberFailure {
    SessionExpired,
    Offline,
    InvalidToken,
    Unknown,
}

fun linkMemberFailureForHttpStatus(
    statusCode: Int,
    invalidTokenOnClientError: Boolean,
): LinkMemberFailure =
    when (statusCode) {
        401, 403 -> LinkMemberFailure.SessionExpired
        in 400..499 ->
            if (invalidTokenOnClientError) {
                LinkMemberFailure.InvalidToken
            } else {
                LinkMemberFailure.Unknown
            }
        else -> LinkMemberFailure.Unknown
    }

class LinkMemberException(
    val reason: LinkMemberFailure,
    cause: Throwable? = null,
) : IllegalStateException(reason.name, cause)
