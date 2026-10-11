package com.github.apkelly.drool.common.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.content_splash_animation
import com.github.apkelly.drool.common.resources.splash_loading
import io.github.alexzhirkevich.compottie.ExperimentalCompottieApi
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.Resource
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalCompottieApi::class)
@Composable
fun SplashContent(
    onAnimationFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnAnimationFinish by rememberUpdatedState(onAnimationFinish)
    val compositionResult = rememberLottieComposition {
        LottieCompositionSpec.Resource(
            path = "files/splash_trophy.lottie",
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
            currentOnAnimationFinish()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
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
                    .fillMaxWidth(SPLASH_WIDTH_FRACTION)
                    .widthIn(max = 420.dp)
                    .aspectRatio(SPLASH_ASPECT_RATIO),
            )
        }

        Text(
            text = stringResource(Res.string.splash_loading),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private const val SPLASH_WIDTH_FRACTION = 0.9f
private const val SPLASH_ASPECT_RATIO = 1080f / 808f
