package com.github.apkelly.drool.ui.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
actual fun formatFixtureDateTime(epochMillis: Long): String {
    val context = LocalContext.current
    val date = Date(epochMillis)
    val formattedDate = DateFormat.getMediumDateFormat(context).format(date)
    val formattedTime = DateFormat.getTimeFormat(context).format(date)
    return "$formattedDate, $formattedTime"
}

actual fun formatFixtureDateHeading(epochMillis: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

@Composable
actual fun formatFixtureDay(epochMillis: Long): String =
    SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(epochMillis))

@Composable
actual fun formatFixtureDate(epochMillis: Long): String =
    DateFormat.getMediumDateFormat(LocalContext.current).format(Date(epochMillis))

@Composable
actual fun formatFixtureTime(epochMillis: Long): String =
    DateFormat.getTimeFormat(LocalContext.current).format(Date(epochMillis))
