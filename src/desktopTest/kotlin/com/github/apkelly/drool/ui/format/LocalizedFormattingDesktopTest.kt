package com.github.apkelly.drool.ui.format

import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalizedFormattingDesktopTest {
    @Test
    fun parsesMacOsClockPreference() {
        assertEquals(true, parseMacOs24HourPreference("1"))
        assertEquals(true, parseMacOs24HourPreference("YES"))
        assertEquals(false, parseMacOs24HourPreference("0"))
        assertEquals(false, parseMacOs24HourPreference("false"))
        assertNull(parseMacOs24HourPreference("missing"))
    }

    @Test
    fun formatsTimeUsingSelectedClockCycle() {
        val epochMillis = Instant.parse("2026-10-07T18:05:00Z").toEpochMilli()

        assertEquals(
            "18:05",
            formatDesktopTime(epochMillis, true, Locale.US, ZoneOffset.UTC),
        )
        assertEquals(
            "6:05 PM",
            formatDesktopTime(epochMillis, false, Locale.US, ZoneOffset.UTC),
        )
    }

    @Test
    fun recognizesLocaleClockPatterns() {
        assertTrue(localeUses24HourClock(Locale.FRANCE))
        assertFalse(localeUses24HourClock(Locale.US))
    }
}
