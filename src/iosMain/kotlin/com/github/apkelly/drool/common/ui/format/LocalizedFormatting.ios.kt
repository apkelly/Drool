package com.github.apkelly.drool.common.ui.format

import androidx.compose.runtime.Composable
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterFullStyle
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLock

private val formatterLock = NSLock()

private val dateTimeFormatter by lazy {
    NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
    }
}

private val dateHeadingFormatter by lazy {
    NSDateFormatter().apply {
        dateStyle = NSDateFormatterFullStyle
        timeStyle = NSDateFormatterNoStyle
    }
}

private val dayFormatter by lazy {
    NSDateFormatter().apply {
        dateFormat = "EEEE"
    }
}

private val dateFormatter by lazy {
    NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
    }
}

private val timeFormatter by lazy {
    NSDateFormatter().apply {
        dateStyle = NSDateFormatterNoStyle
        timeStyle = NSDateFormatterShortStyle
    }
}

@Composable
actual fun formatFixtureDateTime(
    epochMillis: Long,
): String = withFormatterLock {
    dateTimeFormatter.stringFromDate(epochMillis.toDate())
}

actual fun formatFixtureDateHeading(
    epochMillis: Long,
): String = withFormatterLock {
    dateHeadingFormatter.stringFromDate(epochMillis.toDate())
}

@Composable
actual fun formatFixtureDay(
    epochMillis: Long,
): String = withFormatterLock {
    dayFormatter.stringFromDate(epochMillis.toDate())
}

@Composable
actual fun formatFixtureDate(
    epochMillis: Long,
): String = withFormatterLock {
    dateFormatter.stringFromDate(epochMillis.toDate())
}

@Composable
actual fun formatFixtureTime(
    epochMillis: Long,
): String = withFormatterLock {
    timeFormatter.stringFromDate(epochMillis.toDate())
}

@Composable
actual fun platformUses24HourClock(): Boolean = withFormatterLock {
    timeFormatter.dateFormat.any { it == 'H' || it == 'k' }
}

private inline fun <T> withFormatterLock(
    block: () -> T,
): T {
    formatterLock.lock()
    return try {
        block()
    } finally {
        formatterLock.unlock()
    }
}

private fun Long.toDate(): NSDate = NSDate(
    timeIntervalSinceReferenceDate = this / 1_000.0 - 978_307_200.0,
)
