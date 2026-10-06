package com.github.apkelly.drool.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val WhistleIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Whistle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(14f, 7f)
            curveTo(18.4f, 7f, 22f, 10.1f, 22f, 14f)
            curveTo(22f, 17.9f, 18.4f, 21f, 14f, 21f)
            curveTo(10.1f, 21f, 7f, 18.2f, 7f, 14.5f)
            lineTo(2f, 13f)
            lineTo(2f, 8f)
            lineTo(10f, 11f)
            curveTo(11.1f, 8.5f, 12.2f, 7f, 14f, 7f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(16.5f, 14f)
            curveTo(16.5f, 15.4f, 15.4f, 16.5f, 14f, 16.5f)
            curveTo(12.6f, 16.5f, 11.5f, 15.4f, 11.5f, 14f)
            curveTo(11.5f, 12.6f, 12.6f, 11.5f, 14f, 11.5f)
            curveTo(15.4f, 11.5f, 16.5f, 12.6f, 16.5f, 14f)
            close()
        }
    }.build()
