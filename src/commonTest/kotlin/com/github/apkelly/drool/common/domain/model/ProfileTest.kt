package com.github.apkelly.drool.common.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileTest {
    @Test
    fun familyProfilesContainPrimaryAndDistinctLinkedSubjectsOnly() {
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

        assertEquals(listOf("root", "child"), profile.familyProfiles.map { it.id })
        assertEquals("Jude", profile.familyProfiles[1].displayName)
    }
}
