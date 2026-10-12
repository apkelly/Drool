package com.github.apkelly.drool

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.apkelly.drool.app.ui.DroolApp
import com.github.apkelly.drool.app.ui.viewmodel.AppViewModel
import com.github.apkelly.drool.common.di.initDroolKoin
import com.github.apkelly.drool.common.ui.theme.DroolTheme
import com.github.apkelly.drool.discover.ui.viewmodel.DiscoverViewModel
import com.github.apkelly.drool.match.ui.viewmodel.MatchViewModel
import com.github.apkelly.drool.schedule.ui.viewmodel.AvailabilityViewModel
import com.github.apkelly.drool.schedule.ui.viewmodel.ScheduleViewModel
import com.github.apkelly.drool.team.ui.viewmodel.TeamViewModel

@Composable
@Suppress("ViewModelForwarding")
fun App(
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel = viewModel { initDroolKoin().get() },
    discoverViewModel: DiscoverViewModel = viewModel { initDroolKoin().get() },
    scheduleViewModel: ScheduleViewModel = viewModel { initDroolKoin().get() },
    teamViewModel: TeamViewModel = viewModel { initDroolKoin().get() },
    matchViewModel: MatchViewModel = viewModel { initDroolKoin().get() },
    availabilityViewModel: AvailabilityViewModel = viewModel {
        initDroolKoin().get()
    },
) {
    val themeMode by appViewModel.themeMode.collectAsState()

    DroolTheme(themeMode) {
        Surface(modifier = modifier) {
            DroolApp(
                appViewModel = appViewModel,
                discoverViewModel = discoverViewModel,
                scheduleViewModel = scheduleViewModel,
                teamViewModel = teamViewModel,
                matchViewModel = matchViewModel,
                availabilityViewModel = availabilityViewModel,
            )
        }
    }
}
