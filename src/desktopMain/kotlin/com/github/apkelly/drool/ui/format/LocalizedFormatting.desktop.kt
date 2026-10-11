package com.github.apkelly.drool.ui.format

import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.chrono.IsoChronology
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.github.apkelly.drool.logging.DroolLog

private val formattingLogger = DroolLog.withTag("LocalizedFormatting")

@Composable
actual fun formatFixtureDateTime(epochMillis: Long): String =
    "${formatDesktopDate(epochMillis)}, ${formatDesktopTime(epochMillis)}"

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
    formatDesktopDate(epochMillis)

@Composable
actual fun formatFixtureTime(epochMillis: Long): String =
    formatDesktopTime(epochMillis)

private val desktopUses24HourClock by lazy {
    readMacOs24HourPreference()
        ?: localeUses24HourClock(Locale.getDefault())
}

private fun formatDesktopDate(epochMillis: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

internal fun formatDesktopTime(
    epochMillis: Long,
    use24HourClock: Boolean = desktopUses24HourClock,
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String =
    DateTimeFormatter.ofPattern(
        if (use24HourClock) "HH:mm" else "h:mm a",
        locale,
    )
        .withZone(zoneId)
        .format(Instant.ofEpochMilli(epochMillis))

internal fun parseMacOs24HourPreference(value: String): Boolean? =
    when (value.trim().lowercase()) {
        "1", "true", "yes" -> true
        "0", "false", "no" -> false
        else -> null
    }

internal fun localeUses24HourClock(locale: Locale): Boolean {
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
