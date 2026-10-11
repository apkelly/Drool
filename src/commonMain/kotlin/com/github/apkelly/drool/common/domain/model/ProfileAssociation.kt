package com.github.apkelly.drool.common.domain.model

import kotlinx.serialization.Serializable

data class RelatedUser(
    val id: String,
    val displayName: String,
    val email: String?,
    val avatarUrl: String?,
    val subjectUserId: String = id,
    val linkId: String? = null,
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

/**
 * Identifies one selectable sports profile within the authenticated account.
 *
 * [id] is the stable local selector/cache key. [subjectUserId] is the backend person ID used for
 * user-scoped API queries.
 */
data class LinkedProfile(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val isPrimary: Boolean,
    val subjectUserId: String = id,
)

/**
 * Finds the profile representing the signed-in user.
 *
 * Linked-profile endpoints can return the primary profile in any position, so callers must use the
 * explicit marker rather than assuming the first profile belongs to the account owner.
 */
fun List<LinkedProfile>.primaryProfileId(): String? = firstOrNull(LinkedProfile::isPrimary)?.id

data class Account(
    val id: String,
    val name: String,
    val subtitle: String?,
    val logoUrl: String?,
)

data class LinkMemberRequestResult(
    val relationshipId: String,
    val isApproved: Boolean,
)

enum class LinkMemberFailure {
    SessionExpired,
    Offline,
    InvalidToken,
    Unknown,
}

/**
 * Converts the linked-member endpoint's HTTP status into a user-facing failure category.
 *
 * Authentication failures always expire the current session. Other client errors represent an
 * invalid link token only for endpoint operations where the server uses the 4xx range for token
 * validation; [invalidTokenOnClientError] keeps that endpoint-specific contract explicit.
 */
fun linkMemberFailureForHttpStatus(
    statusCode: Int,
    invalidTokenOnClientError: Boolean,
): LinkMemberFailure = when (statusCode) {
    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> LinkMemberFailure.SessionExpired

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

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
