package com.github.apkelly.drool.ui.format

import androidx.compose.runtime.Composable

expect fun formatFixtureDateTime(epochMillis: Long): String
expect fun formatFixtureDateHeading(epochMillis: Long): String

@Composable
expect fun formatFixtureDay(epochMillis: Long): String

@Composable
expect fun formatFixtureDate(epochMillis: Long): String

@Composable
expect fun formatFixtureTime(epochMillis: Long): String
