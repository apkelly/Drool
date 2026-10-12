package com.github.apkelly.drool.profile.ui

import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerHeroCardTest {
    @Test
    fun `fantasy card is stable for a player`() {
        val user = relatedUser("player-42")

        assertEquals(fantasyPlayerCard(user), fantasyPlayerCard(user))
    }

    @Test
    fun `fantasy card values stay in their display ranges`() {
        val card = fantasyPlayerCard(relatedUser("player-73"))

        assertTrue(card.jerseyNumber in 1..30)
        assertTrue(card.attack in 64..97)
        assertTrue(card.midfield in 64..97)
        assertTrue(card.defence in 64..97)
        assertEquals(card.attack + card.midfield + card.defence, card.total)
        assertTrue(card.cardNumber.matches(Regex("#\\d{3}")))
    }

    @Test
    fun `different stable identifiers produce different cards`() {
        assertNotEquals(
            fantasyPlayerCard(relatedUser("player-11")),
            fantasyPlayerCard(relatedUser("player-12")),
        )
    }

    @Test
    fun `gender values use compact known labels and preserve other values`() {
        assertEquals("M", playerCardGender("Male"))
        assertEquals("F", playerCardGender("female"))
        assertEquals("NB", playerCardGender("non-binary"))
        assertEquals("X", playerCardGender("X"))
        assertEquals("OTH", playerCardGender("Other"))
        assertNull(playerCardGender(null))
        assertNull(playerCardGender(" "))
        assertNull(playerCardGender("Unknown"))
        assertNull(playerCardGender("Not specified"))
    }

    @Test
    fun `staff relationships replace player presentation`() {
        assertEquals(
            PlayerCardRole.Coach,
            playerCardRole(listOf(TeamRelationship.Player, TeamRelationship.Coaching)),
        )
        assertEquals(
            PlayerCardRole.Manager,
            playerCardRole(listOf(TeamRelationship.Coaching, TeamRelationship.Managing)),
        )
        assertEquals(
            PlayerCardRole.Player,
            playerCardRole(listOf(TeamRelationship.Player)),
        )
    }

    private fun relatedUser(
        id: String,
    ) = RelatedUser(
        id = id,
        subjectUserId = id,
        displayName = "Trial Player",
        email = null,
        avatarUrl = null,
    )
}
