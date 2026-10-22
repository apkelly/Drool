package com.github.apkelly.drool.common.ui.widgets

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SportsCardsTest {
    @Test
    fun sportsColorsUseArgbColorSpace() {
        val sixDigit = "#123456".toColorOrNull()
        val eightDigit = "80123456".toColorOrNull()

        assertEquals(Color(0xFF123456.toInt()), sixDigit)
        assertEquals(Color(0x80123456.toInt()), eightDigit)
        assertTrue(checkNotNull(sixDigit).luminance() in 0f..1f)
        assertTrue(checkNotNull(eightDigit).luminance() in 0f..1f)
        assertNull("#12345".toColorOrNull())
        assertNull("not-a-color".toColorOrNull())
    }

    @Test
    fun secondPitchToneCreatesDistinctContrastToneForLightAndDarkSurfaces() {
        val lightTone = Color(0xFFD2EEDB)
        val darkTone = Color(0xFF143020)

        val lightSecondary = lightTone.secondPitchTone()
        val darkSecondary = darkTone.secondPitchTone()

        assertTrue(lightSecondary.luminance() < lightTone.luminance())
        assertTrue(darkSecondary.luminance() > darkTone.luminance())
    }

    @Test
    fun turfStripesMaintainConsistentSlopeAcrossCardDimensions() {
        val compactCard = computeTurfStripeCoordinates(width = 372f, height = 72f)
        val mediumCard = computeTurfStripeCoordinates(width = 372f, height = 180f)
        val tallNarrowCard = computeTurfStripeCoordinates(width = 200f, height = 320f)

        val compactSlope = (compactCard.splitStartTopX - compactCard.splitStartBottomX) / 72f
        val mediumSlope = (mediumCard.splitStartTopX - mediumCard.splitStartBottomX) / 180f
        val tallSlope = (tallNarrowCard.splitStartTopX - tallNarrowCard.splitStartBottomX) / 320f
        val bandSlope = (mediumCard.bandStartTopX - mediumCard.bandStartBottomX) / 180f

        assertEquals(compactSlope, mediumSlope, absoluteTolerance = 0.0001f)
        assertEquals(mediumSlope, tallSlope, absoluteTolerance = 0.0001f)
        assertEquals(mediumSlope, bandSlope, absoluteTolerance = 0.0001f)
    }

    @Test
    fun pitchCornerBorderExtendsFullEdgesWithConsistentArcRadius() {
        val compact = computePitchCornerBorderGeometry(
            width = 360f,
            height = 70f,
            strokeWidthPx = 4f,
            arcRadiusPx = 88f,
        )
        val tall = computePitchCornerBorderGeometry(
            width = 240f,
            height = 300f,
            strokeWidthPx = 4f,
            arcRadiusPx = 88f,
        )

        assertEquals(358f, compact.cornerX, absoluteTolerance = 0.0001f)
        assertEquals(2f, compact.cornerY, absoluteTolerance = 0.0001f)
        assertEquals(0f, compact.topLineStartX, absoluteTolerance = 0.0001f)
        assertEquals(70f, compact.rightLineEndY, absoluteTolerance = 0.0001f)
        assertEquals(0f, tall.topLineStartX, absoluteTolerance = 0.0001f)
        assertEquals(300f, tall.rightLineEndY, absoluteTolerance = 0.0001f)
        assertEquals(compact.arcRadius, tall.arcRadius, absoluteTolerance = 0.0001f)
    }

    @Test
    fun grassTextureGeneratesHighlightAndShadowBladePaths() {
        val paths = buildGrassTexturePaths(
            width = 220f,
            height = 110f,
            cellStepPx = 22f,
            bladeHeightPx = 10f,
            bladeSwayPx = 4f,
        )

        assertEquals(50, paths.tuftCount)
        assertTrue(!paths.highlightBladesPath.isEmpty)
        assertTrue(!paths.shadowBladesPath.isEmpty)
    }
}
