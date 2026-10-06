package com.github.apkelly.drool.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals

class TeamNameFormatTest {
    @Test
    fun removesASharedLeaguePrefix() {
        assertEquals(
            FixtureTeamNames("La Masia FC", "The Strongest FC"),
            displayTeamNames(
                "Marrickville Under 6 Mixed La Masia FC",
                "Marrickville Under 6 Mixed The Strongest FC",
                listOf("Marrickville F5s", "Under 6 Mixed Black Mixed"),
            ),
        )
    }

    @Test
    fun retainsSharedWordsAfterTheLeagueContext() {
        assertEquals(
            FixtureTeamNames("Sheffield Wednesday", "Sheffield United"),
            displayTeamNames(
                "Marrickville Under 13 Mixed Sheffield Wednesday",
                "Marrickville Under 13 Mixed Sheffield United",
                listOf("Marrickville F5s", "Under 13 Mixed White Mixed"),
            ),
        )
        assertEquals(
            FixtureTeamNames("Sheffield Wednesday", "Sheffield Wednesday Reserves"),
            displayTeamNames(
                "Marrickville Under 13 Mixed Sheffield Wednesday",
                "Marrickville Under 13 Mixed Sheffield Wednesday Reserves",
                listOf("Marrickville F5s", "Under 13 Mixed White Mixed"),
            ),
        )
    }

    @Test
    fun removesOnlySharedPrefixWordsPresentInTheNamingContext() {
        assertEquals(
            FixtureTeamNames("Sydney Olympic", "Sydney United"),
            displayTeamNames(
                "Marrickville Sydney Olympic",
                "Marrickville Sydney United",
                listOf("Marrickville Premier League"),
            ),
        )
        assertEquals(
            FixtureTeamNames("Marrickville Under 13 Mixed", "Marrickville Under 13 Mixed"),
            displayTeamNames(
                "Marrickville Under 13 Mixed",
                "Marrickville Under 13 Mixed",
                emptyList(),
            ),
        )
    }
}
