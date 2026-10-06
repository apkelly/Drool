package com.github.apkelly.drool.data.weather

import com.github.apkelly.drool.domain.model.MatchWeather

interface WeatherRemoteDataSource {
    suspend fun fetchMatchWeather(
        apiKey: String,
        latitude: Double,
        longitude: Double,
        kickoffEpochMillis: Long,
    ): MatchWeather?
}
