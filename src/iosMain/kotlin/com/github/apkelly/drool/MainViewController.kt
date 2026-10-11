package com.github.apkelly.drool

import androidx.compose.ui.window.ComposeUIViewController
import com.github.apkelly.drool.common.di.initDroolKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initDroolKoin()
    return ComposeUIViewController {
        App()
    }
}
