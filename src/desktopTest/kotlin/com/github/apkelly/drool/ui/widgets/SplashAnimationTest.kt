package com.github.apkelly.drool.ui.widgets

import io.github.alexzhirkevich.compottie.LottieComposition
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertEquals

class SplashAnimationTest {
    @Test
    fun trophyDotLottieContainsACompleteCompottieAnimation() {
        val file = File("src/commonMain/composeResources/files/splash_trophy.lottie")
        val animationJson = ZipFile(file).use { archive ->
            val animation = archive.entries().asSequence()
                .single { it.name.startsWith("animations/") && it.name.endsWith(".json") }
            archive.getInputStream(animation).bufferedReader().readText()
        }
        val composition = LottieComposition.parse(animationJson)

        assertEquals(180f, composition.durationFrames)
        assertEquals(30f, composition.frameRate)
        assertEquals(6f, composition.durationFrames / composition.frameRate)
    }
}
