package com.github.apkelly.drool.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.ExperimentalCompottieApi
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.Resource
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.content_splash_animation
import com.github.apkelly.drool.resources.splash_loading
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalCompottieApi::class)
@Composable
fun SplashContent(onAnimationFinished: () -> Unit) {
    val compositionResult = rememberLottieComposition {
        LottieCompositionSpec.Resource(
            path = "files/splash_pitch.json",
            reader = Res::readBytes,
        )
    }
    val composition by compositionResult
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
    )
    LaunchedEffect(progress, compositionResult.isFailure) {
        if (progress >= 1f || compositionResult.isFailure) {
            onAnimationFinished()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        composition?.let {
            Image(
                painter = rememberLottiePainter(
                    composition = it,
                    progress = { progress },
                ),
                contentDescription = stringResource(Res.string.content_splash_animation),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 420.dp)
                    .aspectRatio(1f),
            )
        }
        Text(
            text = stringResource(Res.string.splash_loading),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
