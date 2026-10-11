package com.github.apkelly.drool.match.data.weather

import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.MatchWeather

interface MatchWeatherDataSource {
    suspend fun loadMatchWeather(fixture: Fixture): MatchWeather?
}
