package com.github.apkelly.drool.domain.repository

import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.MatchWeather

interface MatchWeatherRepository {
    suspend fun loadMatchWeather(fixture: Fixture): MatchWeather?
}
