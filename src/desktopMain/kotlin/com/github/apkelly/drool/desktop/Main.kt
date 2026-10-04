package com.github.apkelly.drool.desktop

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension
import com.github.apkelly.drool.App
import com.github.apkelly.drool.di.initDroolKoin

fun main() {
    System.setProperty("apple.awt.application.name", "Drool")
    initDroolKoin()
    application {
        Window(onCloseRequest = ::exitApplication, title = "Drool") {
            SideEffect {
                window.minimumSize = Dimension(
                    MINIMUM_WINDOW_WIDTH,
                    MINIMUM_WINDOW_HEIGHT,
                )
                window.maximumSize = Dimension(
                    MAXIMUM_WINDOW_WIDTH,
                    MAXIMUM_WINDOW_HEIGHT,
                )
            }
            App()
        }
    }
}

private const val MINIMUM_WINDOW_WIDTH = 640
private const val MINIMUM_WINDOW_HEIGHT = 600
private const val MAXIMUM_WINDOW_WIDTH = 1_600
private const val MAXIMUM_WINDOW_HEIGHT = 1_200
