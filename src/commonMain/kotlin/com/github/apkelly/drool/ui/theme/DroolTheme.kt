package com.github.apkelly.drool.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.github.apkelly.drool.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF087A46),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC0F2D4),
    onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFF506352),
    tertiary = Color(0xFF5B6300),
    background = Color(0xFFF5F8F5),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF43C983),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF00522E),
    onPrimaryContainer = Color(0xFFC0F2D4),
    secondary = Color(0xFFB7CCBA),
    tertiary = Color(0xFFD7E64A),
    background = Color(0xFF0D1511),
    surface = Color(0xFF16211B),
    error = Color(0xFFFFB4AB),
)

@Composable
fun DroolTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
