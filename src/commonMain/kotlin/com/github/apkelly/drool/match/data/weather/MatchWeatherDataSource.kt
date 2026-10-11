package com.github.apkelly.drool.match.data.weather

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.MatchWeather

interface MatchWeatherDataSource {
    suspend fun loadMatchWeather(
        fixture: Fixture,
        force: Boolean = false,
    ): MatchWeather?
}
