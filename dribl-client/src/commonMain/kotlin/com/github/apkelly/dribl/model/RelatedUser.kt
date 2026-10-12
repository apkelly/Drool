package com.github.apkelly.dribl.model

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
