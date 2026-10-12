package com.github.apkelly.drool.common.data.mapper

import com.github.apkelly.dribl.model.normalizedTeamName
import com.github.apkelly.drool.common.data.local.entity.FixtureEntity
import com.github.apkelly.drool.common.data.local.entity.TeamEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class TeamNameMapperTest {
    @Test
    fun removesOnlyACompleteCompetitionPrefix() {
        assertEquals(
            "Sheffield Wednesday",
            normalizedTeamName(
                "Marrickville  Under 13 Mixed - Sheffield Wednesday",
                "marrickville under 13 mixed",
            ),
        )
        assertEquals(
            "Marrickville Under 13 Mixedwood",
            normalizedTeamName(
                "Marrickville Under 13 Mixedwood",
                "Marrickville Under 13 Mixed",
            ),
        )
        assertEquals(
            "Marrickville Under 13 Mixed",
            normalizedTeamName(
                "Marrickville Under 13 Mixed",
                "Marrickville Under 13 Mixed",
            ),
        )
    }

    @Test
    fun usesTheLongestMatchingLeagueSegmentForLadderNames() {
        assertEquals(
            "Cobh RAMBLERS",
            normalizedTeamName(
                name = "Marrickville Under 13 Mixed  Cobh RAMBLERS",
                competitionName = "Under 13 Mixed White Mixed",
                allowEmbeddedLeaguePrefix = true,
            ),
        )
    }

    @Test
    fun normalizesPreviouslyCachedTeamsAndFixtures() {
        val team = TeamEntity(
            id = "team",
            clubId = "club",
            name = "Marrickville Under 13 Mixed Sheffield Wednesday",
            shortName = null,
            logoUrl = null,
            ageGroup = "Under 13",
            competitionName = "Marrickville Under 13 Mixed",
            active = true,
        ).toDomain()
        val fixture = FixtureEntity(
            id = "fixture",
            accountId = "account",
            kickoffEpochMillis = 0,
            homeTeamId = "home",
            homeTeamName = "Marrickville Under 13 Mixed Sheffield Wednesday",
            awayTeamId = "away",
            awayTeamName = "Marrickville Under 13 Mixed Cobh RAMBLERS",
            competitionName = "Under 13 Mixed White Mixed",
            venueName = null,
            userTeamId = null,
            status = "Scheduled",
            leagueName = "Under 13 Mixed White Mixed",
            homeScore = 2,
            awayScore = 1,
            homeTeamLogoUrl = "home.png",
            awayTeamLogoUrl = "away.png",
        ).toDomain()

        assertEquals("Sheffield Wednesday", team.name)
        assertEquals("Sheffield Wednesday", fixture.homeTeamName)
        assertEquals("Cobh RAMBLERS", fixture.awayTeamName)
        assertEquals("Under 13 Mixed White Mixed", fixture.leagueName)
        assertEquals(2, fixture.homeScore)
        assertEquals(1, fixture.awayScore)
        assertEquals("home.png", fixture.homeTeamLogoUrl)
        assertEquals("away.png", fixture.awayTeamLogoUrl)
    }
}
