package com.github.apkelly.drool.ui.widgets

import io.github.alexzhirkevich.compottie.LottieComposition
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SplashAnimationTest {
    @Test
    fun bouncingBallAnimationParsesWithCompottie() {
        val composition = LottieComposition.parse(
            File("src/commonMain/composeResources/files/splash_pitch.json").readText()
        )

        assertEquals(90f, composition.durationFrames)
        assertEquals(30f, composition.frameRate)
    }
}
