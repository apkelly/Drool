package com.github.apkelly.drool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.github.apkelly.drool.common.data.storage.initializeAndroidStorage
import com.github.apkelly.drool.common.di.initDroolKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        initializeAndroidStorage(applicationContext)
        initDroolKoin()
        setContent {
            App()
        }
    }
}
