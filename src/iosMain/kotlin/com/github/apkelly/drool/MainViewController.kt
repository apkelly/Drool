package com.github.apkelly.drool

import androidx.compose.ui.window.ComposeUIViewController
import com.github.apkelly.drool.di.initDroolKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initDroolKoin()
    return ComposeUIViewController {
        App()
    }
}
