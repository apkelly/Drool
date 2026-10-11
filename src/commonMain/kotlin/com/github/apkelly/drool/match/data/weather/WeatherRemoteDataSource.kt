package com.github.apkelly.drool.match.data.weather

import com.github.apkelly.drool.common.domain.model.MatchWeather

interface WeatherRemoteDataSource {
    suspend fun fetchMatchWeather(
        apiKey: String,
        latitude: Double,
        longitude: Double,
        kickoffEpochMillis: Long,
    ): MatchWeather?
}
