package com.github.apkelly.dribl.model

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

data class Account(
    val id: String,
    val name: String,
    val subtitle: String?,
    val logoUrl: String?,
)
