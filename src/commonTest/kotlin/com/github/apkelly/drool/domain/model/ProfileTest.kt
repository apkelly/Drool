package com.github.apkelly.drool.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileTest {
    @Test
    fun familyProfilesContainPrimaryAndDistinctLinkedSubjectsOnly() {
        val profile = Profile(
            accountId = "root",
            displayName = "Andrew",
            email = "andrew@example.com",
            relatedUsers = listOf(
                RelatedUser(
                    id = "related-login",
                    displayName = "Andrew",
                    email = "andrew@example.com",
                    avatarUrl = null,
                ),
                RelatedUser(
                    id = "relationship-1",
                    displayName = "Ethan",
                    email = null,
                    avatarUrl = "ethan.png",
                    subjectUserId = "child",
                    isLinked = true,
                ),
                RelatedUser(
                    id = "relationship-2",
                    displayName = "Ethan duplicate",
                    email = null,
                    avatarUrl = null,
                    subjectUserId = "child",
                    isLinked = true,
                ),
            ),
        )

        assertEquals(listOf("root", "child"), profile.familyProfiles.map { it.id })
        assertEquals("Ethan", profile.familyProfiles[1].displayName)
    }
}
