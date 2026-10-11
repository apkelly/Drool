package com.github.apkelly.drool

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.github.apkelly.drool.app.ui.DroolApp
import com.github.apkelly.drool.app.ui.viewmodel.AppViewModel
import com.github.apkelly.drool.app.ui.viewmodel.SportsViewModel
import com.github.apkelly.drool.di.initDroolKoin
import com.github.apkelly.drool.schedule.ui.viewmodel.AvailabilityViewModel
import com.github.apkelly.drool.ui.theme.DroolTheme

@Composable
fun App(
    modifier: Modifier = Modifier,
) {
    val koin = remember { initDroolKoin() }
    val appViewModel = remember(koin) { koin.get<AppViewModel>() }
    val sportsViewModel = remember(koin) { koin.get<SportsViewModel>() }
    val availabilityViewModel = remember(koin) { koin.get<AvailabilityViewModel>() }
    val themeMode by appViewModel.themeMode.collectAsState()

    DroolTheme(themeMode) {
        Surface(modifier = modifier) {
            DroolApp(
                appViewModel = appViewModel,
                sportsViewModel = sportsViewModel,
                availabilityViewModel = availabilityViewModel,
            )
        }
    }
}
