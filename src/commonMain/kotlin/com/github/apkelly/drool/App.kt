package com.github.apkelly.drool

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.github.apkelly.drool.di.initDroolKoin
import com.github.apkelly.drool.ui.screens.DroolApp
import com.github.apkelly.drool.ui.theme.DroolTheme
import com.github.apkelly.drool.ui.viewmodel.AppViewModel
import com.github.apkelly.drool.ui.viewmodel.SportsViewModel

@Composable
fun App() {
    val koin = remember { initDroolKoin() }
    val appViewModel = remember(koin) { koin.get<AppViewModel>() }
    val sportsViewModel = remember(koin) { koin.get<SportsViewModel>() }
    val themeMode by appViewModel.themeMode.collectAsState()

    DroolTheme(themeMode) {
        Surface {
            DroolApp(
                appViewModel = appViewModel,
                sportsViewModel = sportsViewModel,
            )
        }
    }
}
