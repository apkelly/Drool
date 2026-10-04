package com.github.apkelly.drool.ui.widgets

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
}
