package com.github.apkelly.drool.data.weather

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.github.apkelly.drool.data.local.DroolDatabase
import com.github.apkelly.drool.data.local.entity.MatchWeatherCacheEntity
import com.github.apkelly.drool.data.time.TimeProvider
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.MatchWeather
import com.github.apkelly.drool.domain.repository.MatchWeatherRepository

class MatchWeatherRepositoryImpl(
    private val api: WeatherRemoteDataSource,
    private val database: DroolDatabase,
    private val timeProvider: TimeProvider,
    private val apiKey: String?,
) : MatchWeatherRepository {
    override suspend fun loadMatchWeather(fixture: Fixture): MatchWeather? {
        val latitude = fixture.latitude ?: return null
        val longitude = fixture.longitude ?: return null
        val key = apiKey?.takeIf(String::isNotBlank) ?: return null
        val now = timeProvider.nowEpochMillis()
        val cached = database.matchWeatherCacheDao().get(fixture.id)
        if (cached != null && now - cached.updatedAtEpochMillis < WeatherCacheTtl) {
            return cached.payloadJson?.let(Json::decodeFromString)
        }
        val weather = api.fetchMatchWeather(
            apiKey = key,
            latitude = latitude,
            longitude = longitude,
            kickoffEpochMillis = fixture.kickoffEpochMillis,
        )
        database.matchWeatherCacheDao().upsert(
            MatchWeatherCacheEntity(
                matchId = fixture.id,
                payloadJson = weather?.let { Json.encodeToString(it) },
                updatedAtEpochMillis = now,
            )
        )
        return weather
    }

    private companion object {
        const val WeatherCacheTtl = 24L * 60L * 60L * 1_000L
    }
}
