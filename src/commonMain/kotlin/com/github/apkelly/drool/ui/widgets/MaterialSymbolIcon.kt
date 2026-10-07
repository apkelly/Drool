package com.github.apkelly.drool.ui.widgets

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.material_symbols_outlined
import org.jetbrains.compose.resources.Font

enum class MaterialSymbol(val glyph: String) {
    AccountBalance("\uE84F"),
    Add("\uE145"),
    ArrowBack("\uE5C4"),
    CalendarMonth("\uEBCC"),
    Check("\uE668"),
    CloudOff("\uE2C1"),
    Email("\uE159"),
    Groups("\uF233"),
    Home("\uE9B2"),
    LocationOn("\uF1DB"),
    Person("\uF0D3"),
    Phone("\uF0D4"),
    Refresh("\uE5D5"),
    Search("\uEF7A"),
    Assignment("\uE85D"),
    Sports("\uEA30"),
    SportsSoccer("\uEA2F"),
    Visibility("\uE8F4"),
    VisibilityOff("\uE8F5"),
}

@Composable
fun MaterialSymbolIcon(
    symbol: MaterialSymbol,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    val fontSize = with(LocalDensity.current) { size.toSp() }
    Text(
        text = symbol.glyph,
        modifier = modifier
            .size(size)
            .wrapContentSize(Alignment.Center)
            .clearAndSetSemantics {
                contentDescription?.let {
                    this.contentDescription = it
                    role = Role.Image
                }
            },
        color = tint,
        fontFamily = FontFamily(Font(Res.font.material_symbols_outlined)),
        fontSize = fontSize,
        lineHeight = fontSize,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
fun MaterialBackIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    MaterialSymbolIcon(
        symbol = MaterialSymbol.ArrowBack,
        contentDescription = contentDescription,
        modifier = modifier.graphicsLayer {
            if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
        },
    )
}
