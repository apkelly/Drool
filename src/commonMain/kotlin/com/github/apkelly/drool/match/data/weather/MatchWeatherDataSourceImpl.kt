package com.github.apkelly.drool.match.data.weather

import com.github.apkelly.drool.common.data.local.DroolDatabase
import com.github.apkelly.drool.common.data.local.entity.MatchWeatherCacheEntity
import com.github.apkelly.drool.common.data.time.TimeProvider
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.MatchWeather
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MatchWeatherDataSourceImpl(
    private val api: WeatherRemoteDataSource,
    private val database: DroolDatabase,
    private val timeProvider: TimeProvider,
    private val apiKey: String?,
) : MatchWeatherDataSource {
    override suspend fun loadMatchWeather(
        fixture: Fixture,
        force: Boolean,
    ): MatchWeather? {
        val now = timeProvider.nowEpochMillis()
        val cacheKey = "$CACHE_VERSION:${fixture.id}"
        val cached = database.matchWeatherCacheDao().get(cacheKey)
        if (!force && cached != null && now - cached.updatedAtEpochMillis < WEATHER_CACHE_TTL) {
            return cached.payloadJson?.let(Json::decodeFromString)
        }
        val latitude = fixture.latitude ?: return null
        val longitude = fixture.longitude ?: return null
        val key = apiKey?.takeIf(String::isNotBlank) ?: return null
        val weather = api.fetchMatchWeather(
            apiKey = key,
            latitude = latitude,
            longitude = longitude,
            kickoffEpochMillis = fixture.kickoffEpochMillis,
        )
        if (weather != null) {
            database.matchWeatherCacheDao().upsert(
                MatchWeatherCacheEntity(
                    matchId = cacheKey,
                    payloadJson = Json.encodeToString(weather),
                    updatedAtEpochMillis = now,
                ),
            )
        }
        return weather
    }

    private companion object {
        const val CACHE_VERSION = "kickoff-period-v1"
        const val WEATHER_CACHE_TTL = 24L * 60L * 60L * 1_000L
    }
}
