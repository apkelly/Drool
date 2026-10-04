package com.github.apkelly.drool.domain.model

data class Club(
    val id: String,
    val name: String,
    val shortName: String?,
    val logoUrl: String?,
    val primaryColor: String?,
    val secondaryColor: String?,
)

data class FamilyClub(
    val profileId: String,
    val club: Club,
)
