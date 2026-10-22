package com.github.apkelly.drool.common.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.ThemeMode

@Immutable
data class PitchCardPalette(
    val primaryTone: Color,
    val secondaryTone: Color,
    val contentColor: Color,
    val watermarkColor: Color,
)

private val DroolShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val LightPitchScreenPrimary = Color(0xFFBDD4C4)
private val LightPitchScreenSecondary = Color(0xFFAEC8B6)
private val DarkPitchScreenPrimary = Color(0xFF07130C)
private val DarkPitchScreenSecondary = Color(0xFF0E2116)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B5D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9AC9AA),
    onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF235337),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFAECBB8),
    onSecondaryContainer = Color(0xFF062111),
    tertiary = Color(0xFF705200),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6CF8B),
    onTertiaryContainer = Color(0xFF261900),
    background = Color.Transparent,
    onBackground = Color(0xFF061D0F),
    surface = Color(0xFFD5E5DA),
    onSurface = Color(0xFF061D0F),
    surfaceVariant = Color(0xFFBCD3C3),
    onSurfaceVariant = Color(0xFF244230),
    surfaceContainerLowest = Color(0xFFE4EFE7),
    surfaceContainerLow = Color(0xFFCCE0D2),
    surfaceContainer = Color(0xFFC2D9C9),
    surfaceContainerHigh = Color(0xFFA3C6AE),
    surfaceContainerHighest = Color(0xFF93BA9F),
    outline = Color(0xFF4E6E59),
    outlineVariant = Color(0xFF7FA38B),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4ADE80),
    onPrimary = Color(0xFF00391B),
    primaryContainer = Color(0xFF0D562E),
    onPrimaryContainer = Color(0xFFBDF5D0),
    secondary = Color(0xFF97D3AE),
    onSecondary = Color(0xFF05381D),
    secondaryContainer = Color(0xFF1C4E31),
    onSecondaryContainer = Color(0xFFB2EFC8),
    tertiary = Color(0xFFFACC15),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF4E3D08),
    onTertiaryContainer = Color(0xFFFDE68A),
    background = Color.Transparent,
    onBackground = Color(0xFFE4F5EA),
    surface = Color(0xFF0C1A12),
    onSurface = Color(0xFFE4F5EA),
    surfaceVariant = Color(0xFF1F3B2A),
    onSurfaceVariant = Color(0xFFB0CEBA),
    surfaceContainerLowest = Color(0xFF050C08),
    surfaceContainerLow = Color(0xFF102218),
    surfaceContainer = Color(0xFF142A1E),
    surfaceContainerHigh = Color(0xFF173624),
    surfaceContainerHighest = Color(0xFF204730),
    outline = Color(0xFF6B8E77),
    outlineVariant = Color(0xFF2B5C3F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

object DroolThemeDefaults {
    val pitchCardPalette: PitchCardPalette
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.toPitchCardPalette()
}

internal fun ColorScheme.toPitchCardPalette(): PitchCardPalette {
    val isLight = surface.luminance() > LIGHT_SURFACE_LUMINANCE_THRESHOLD
    return PitchCardPalette(
        primaryTone = surfaceContainerHigh,
        secondaryTone = surfaceContainerHighest,
        contentColor = onSurface,
        watermarkColor = Color.White.copy(
            alpha = if (isLight) LIGHT_WATERMARK_ALPHA else DARK_WATERMARK_ALPHA,
        ),
    )
}

@Composable
fun DroolTheme(
    mode: ThemeMode,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val primaryPitchBackground =
        if (darkTheme) DarkPitchScreenPrimary else LightPitchScreenPrimary
    val secondaryPitchBackground =
        if (darkTheme) DarkPitchScreenSecondary else LightPitchScreenSecondary
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = DroolShapes,
    ) {
        Box(
            modifier = modifier.pitchScreenBackground(
                primaryTone = primaryPitchBackground,
                secondaryTone = secondaryPitchBackground,
            ),
        ) {
            content()
        }
    }
}

fun Modifier.pitchScreenBackground(
    primaryTone: Color,
    secondaryTone: Color,
): Modifier = drawWithCache {
    val horizontalShift = size.height * SCREEN_TURF_STRIPE_SLOPE
    val bandStartTopX = size.width * SCREEN_TURF_BAND_TOP_START_FRACTION
    val bandEndTopX = size.width * SCREEN_TURF_BAND_TOP_END_FRACTION
    val splitStartTopX = size.width * SCREEN_TURF_SPLIT_TOP_FRACTION
    val stripePath = Path().apply {
        moveTo(splitStartTopX, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height)
        lineTo(splitStartTopX - horizontalShift, size.height)
        close()
    }
    val accentBandPath = Path().apply {
        moveTo(bandStartTopX, 0f)
        lineTo(bandEndTopX, 0f)
        lineTo(bandEndTopX - horizontalShift, size.height)
        lineTo(bandStartTopX - horizontalShift, size.height)
        close()
    }
    onDrawBehind {
        drawRect(color = primaryTone)
        drawPath(
            path = accentBandPath,
            color = secondaryTone.copy(alpha = 0.45f),
        )
        drawPath(
            path = stripePath,
            color = secondaryTone,
        )
    }
}

private const val LIGHT_SURFACE_LUMINANCE_THRESHOLD = 0.5f
private const val LIGHT_WATERMARK_ALPHA = 0.85f
private const val DARK_WATERMARK_ALPHA = 0.34f
private const val SCREEN_TURF_STRIPE_SLOPE = 0.36f
private const val SCREEN_TURF_BAND_TOP_START_FRACTION = 0.32f
private const val SCREEN_TURF_BAND_TOP_END_FRACTION = 0.48f
private const val SCREEN_TURF_SPLIT_TOP_FRACTION = 0.60f
