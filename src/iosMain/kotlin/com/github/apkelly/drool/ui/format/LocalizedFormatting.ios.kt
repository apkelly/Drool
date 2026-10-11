package com.github.apkelly.drool.ui.format

import androidx.compose.runtime.Composable
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterFullStyle
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle

@Composable
actual fun formatFixtureDateTime(epochMillis: Long): String =
    NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
    }.stringFromDate(
        NSDate(timeIntervalSinceReferenceDate = epochMillis / 1_000.0 - 978_307_200.0)
    )

actual fun formatFixtureDateHeading(epochMillis: Long): String =
    NSDateFormatter().apply {
        dateStyle = NSDateFormatterFullStyle
        timeStyle = NSDateFormatterNoStyle
    }.stringFromDate(
        NSDate(timeIntervalSinceReferenceDate = epochMillis / 1_000.0 - 978_307_200.0)
    )

@Composable
actual fun formatFixtureDay(epochMillis: Long): String =
    NSDateFormatter().apply {
        dateFormat = "EEEE"
    }.stringFromDate(epochMillis.toDate())

@Composable
actual fun formatFixtureDate(epochMillis: Long): String =
    NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
    }.stringFromDate(epochMillis.toDate())

@Composable
actual fun formatFixtureTime(epochMillis: Long): String =
    NSDateFormatter().apply {
        dateStyle = NSDateFormatterNoStyle
        timeStyle = NSDateFormatterShortStyle
    }.stringFromDate(epochMillis.toDate())

private fun Long.toDate(): NSDate =
    NSDate(timeIntervalSinceReferenceDate = this / 1_000.0 - 978_307_200.0)
