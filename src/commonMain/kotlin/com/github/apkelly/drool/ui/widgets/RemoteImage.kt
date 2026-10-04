package com.github.apkelly.drool.ui.widgets

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String,
    fallbackIcon: ImageVector,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val fallbackPainter = rememberVectorPainter(fallbackIcon)
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            placeholder = fallbackPainter,
            error = fallbackPainter,
            fallback = fallbackPainter,
            contentScale = contentScale,
        )
    }
}
