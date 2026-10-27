package com.github.apkelly.drool.common.ui.format

import androidx.compose.runtime.Composable
import com.github.apkelly.drool.common.logging.DroolLog
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private val formattingLogger = DroolLog.withTag("LocalizedFormatting")
private val fullDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
private val mediumDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val shortDayMonthFormatter = DateTimeFormatter.ofPattern("d MMM")
private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE")

private val macOs24HourPreferenceFuture: CompletableFuture<Boolean?> =
    CompletableFuture.supplyAsync(::readMacOs24HourPreference)

@Composable
actual fun formatFixtureDateTime(
    epochMillis: Long,
): String = formatDesktopDateTime(epochMillis)

actual fun formatFixtureDateHeading(
    epochMillis: Long,
): String = fullDateFormatter
    .withLocale(Locale.getDefault())
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(epochMillis))

@Composable
actual fun formatFixtureDay(
    epochMillis: Long,
): String = dayOfWeekFormatter
    .withLocale(Locale.getDefault())
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(epochMillis))

@Composable
actual fun formatFixtureDate(
    epochMillis: Long,
): String = formatDesktopDate(epochMillis)

@Composable
actual fun formatFixtureTime(
    epochMillis: Long,
): String = formatDesktopTime(epochMillis)

@Composable
actual fun platformUses24HourClock(): Boolean = desktopUses24HourClock()

private fun desktopUses24HourClock(): Boolean = macOs24HourPreferenceFuture.getNow(null)
    ?: localeUses24HourClock(Locale.getDefault())

private fun formatDesktopDate(
    epochMillis: Long,
): String = mediumDateFormatter
    .withLocale(Locale.getDefault())
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(epochMillis))

internal fun formatDesktopDateTime(
    epochMillis: Long,
    use24HourClock: Boolean = desktopUses24HourClock(),
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val datePart = shortDayMonthFormatter
        .withLocale(locale)
        .withZone(zoneId)
        .format(Instant.ofEpochMilli(epochMillis))
    val timePart = formatDesktopTime(epochMillis, use24HourClock, locale, zoneId)
    return "$datePart - $timePart"
}

internal fun formatDesktopTime(
    epochMillis: Long,
    use24HourClock: Boolean = desktopUses24HourClock(),
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateTimeFormatter.ofPattern(
    if (use24HourClock) "HH:mm" else "h:mm a",
    locale,
)
    .withZone(zoneId)
    .format(Instant.ofEpochMilli(epochMillis))

internal fun parseMacOs24HourPreference(
    value: String,
): Boolean? = when (value.trim().lowercase()) {
    "1", "true", "yes" -> true
    "0", "false", "no" -> false
    else -> null
}

internal fun localeUses24HourClock(
    locale: Locale,
): Boolean {
    val pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
        null,
        FormatStyle.SHORT,
        IsoChronology.INSTANCE,
        locale,
    )
    return pattern.any { it == 'H' || it == 'k' }
}

private fun readMacOs24HourPreference(): Boolean? {
    if (!System.getProperty("os.name").startsWith("Mac", ignoreCase = true)) return null
    return try {
        val process = ProcessBuilder(
            "/usr/bin/defaults",
            "read",
            "-g",
            "AppleICUForce24HourTime",
        )
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(1, TimeUnit.SECONDS)) {
            process.destroy()
            formattingLogger.w { "Timed out reading the macOS clock preference" }
            return null
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (process.exitValue() == 0) parseMacOs24HourPreference(output) else null
    } catch (error: InterruptedException) {
        Thread.currentThread().interrupt()
        formattingLogger.w { "Interrupted while reading the macOS clock preference" }
        null
    } catch (error: IOException) {
        formattingLogger.w {
            "Unable to read the macOS clock preference (${error::class.simpleName})"
        }
        null
    } catch (error: SecurityException) {
        formattingLogger.w {
            "Access to the macOS clock preference was denied (${error::class.simpleName})"
        }
        null
    }
}
