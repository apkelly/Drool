package com.github.apkelly.drool.ui.format

import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

actual fun formatFixtureDateTime(epochMillis: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

actual fun formatFixtureDateHeading(epochMillis: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

@Composable
actual fun formatFixtureDay(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("EEEE")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

@Composable
actual fun formatFixtureDate(epochMillis: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

@Composable
actual fun formatFixtureTime(epochMillis: Long): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))
