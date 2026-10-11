package com.github.apkelly.drool.common.ui.format

import androidx.compose.runtime.Composable

/**
 * Formats a fixture instant as a complete, locale-aware date and time label.
 *
 * Formatting is platform-specific so schedules follow the user's regional conventions rather
 * than imposing one date order or clock style across Android, iOS, and desktop.
 */
@Composable
expect fun formatFixtureDateTime(
    epochMillis: Long,
): String

/**
 * Formats an instant as the section heading used to group fixtures by calendar date.
 */
expect fun formatFixtureDateHeading(
    epochMillis: Long,
): String

/**
 * Formats the localized weekday component of a fixture instant.
 */
@Composable
expect fun formatFixtureDay(
    epochMillis: Long,
): String

/**
 * Formats the localized calendar-date component of a fixture instant.
 */
@Composable
expect fun formatFixtureDate(
    epochMillis: Long,
): String

/**
 * Formats the localized time component of a fixture instant using platform clock preferences.
 */
@Composable
expect fun formatFixtureTime(
    epochMillis: Long,
): String

/**
 * Reports whether time-entry controls should default to a 24-hour clock on this platform.
 */
@Composable
expect fun platformUses24HourClock(): Boolean
