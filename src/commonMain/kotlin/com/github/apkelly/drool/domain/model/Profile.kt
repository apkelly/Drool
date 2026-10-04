package com.github.apkelly.drool.domain.model

data class Profile(
    val accountId: String,
    val displayName: String,
    val email: String?,
    val avatarUrl: String? = null,
    val relatedUsers: List<RelatedUser> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val playingTeamIds: Set<String> = emptySet(),
) {
    val familyProfiles: List<FamilyProfile>
        get() = buildList {
            add(FamilyProfile(accountId, displayName, avatarUrl, isPrimary = true))
            relatedUsers
                .asSequence()
                .filter { it.isLinked }
                .filter { it.subjectUserId != accountId }
                .distinctBy { it.subjectUserId }
                .forEach {
                    add(
                        FamilyProfile(
                            id = it.subjectUserId,
                            displayName = it.displayName,
                            avatarUrl = it.avatarUrl,
                            isPrimary = false,
                        )
                    )
                }
        }
}

data class AuthenticatedSession(
    val bearerToken: String,
    val profile: Profile?,
)
