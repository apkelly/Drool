package com.github.apkelly.drool.common.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileTest {
    @Test
    fun linkedProfilesContainPrimaryAndDistinctLinkedSubjectsOnly() {
        val profile = Profile(
            accountId = "root",
            displayName = "Harry",
            email = "harry.kane@example.com",
            relatedUsers = listOf(
                RelatedUser(
                    id = "related-login",
                    displayName = "Harry",
                    email = "harry.kane@example.com",
                    avatarUrl = null,
                ),
                RelatedUser(
                    id = "linked-owner",
                    displayName = "Harry Kane",
                    email = null,
                    avatarUrl = "harry.png",
                    subjectUserId = "person-root",
                    isLinked = true,
                    firstName = "Harry",
                    isGuardian = false,
                ),
                RelatedUser(
                    id = "relationship-1",
                    displayName = "Jude",
                    email = null,
                    avatarUrl = "jude.png",
                    subjectUserId = "child",
                    isLinked = true,
                ),
                RelatedUser(
                    id = "relationship-2",
                    displayName = "Jude duplicate",
                    email = null,
                    avatarUrl = null,
                    subjectUserId = "child",
                    isLinked = true,
                ),
            ),
        )

        assertEquals(
            listOf("root", "person-root", "child"),
            profile.linkedProfiles.map { it.id },
        )
        assertEquals("root", profile.linkedProfiles.first().subjectUserId)
        assertEquals("Harry Kane", profile.linkedProfiles[1].displayName)
        assertEquals("Jude", profile.linkedProfiles[2].displayName)
    }
}
