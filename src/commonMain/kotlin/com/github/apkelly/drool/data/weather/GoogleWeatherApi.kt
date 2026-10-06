package com.github.apkelly.drool.data.weather

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import com.github.apkelly.drool.data.remote.requireBody
import com.github.apkelly.drool.domain.model.MatchWeather

class GoogleWeatherApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://weather.googleapis.com/v1",
    private val requestHeaders: Map<String, String> = emptyMap(),
) : WeatherRemoteDataSource {
    override suspend fun fetchMatchWeather(
        apiKey: String,
        latitude: Double,
        longitude: Double,
        kickoffEpochMillis: Long,
    ): MatchWeather? {
        val url = "$baseUrl/forecast/days:lookup"
        val response = client.get(url) {
            requestHeaders.forEach { (name, value) -> header(name, value) }
            parameter("key", apiKey)
            parameter("location.latitude", latitude)
            parameter("location.longitude", longitude)
            parameter("days", ForecastDays)
            parameter("pageSize", ForecastDays)
            parameter("unitsSystem", "METRIC")
        }.requireBody<ForecastDaysResponse>(url)
        val kickoff = Instant.fromEpochMilliseconds(kickoffEpochMillis)
        return response.forecastDays
            .firstOrNull { kickoff in it.interval }
            ?.toMatchWeather(kickoff)
    }

    private companion object {
        const val ForecastDays = 10
    }
}

@Serializable
private data class ForecastDaysResponse(
    @SerialName("forecastDays") val forecastDays: List<ForecastDayDto> = emptyList(),
)

@Serializable
private data class ForecastDayDto(
    val interval: ForecastIntervalDto,
    val daytimeForecast: ForecastPeriodDto? = null,
    val nighttimeForecast: ForecastPeriodDto? = null,
    val maxTemperature: TemperatureDto,
    val minTemperature: TemperatureDto,
) {
    fun toMatchWeather(kickoff: Instant): MatchWeather {
        val matchPeriod = listOfNotNull(daytimeForecast, nighttimeForecast)
            .firstOrNull { kickoff in it.interval }
            ?: daytimeForecast
            ?: nighttimeForecast
        val periods = listOfNotNull(daytimeForecast, nighttimeForecast)
        return MatchWeather(
            condition = matchPeriod?.weatherCondition?.description?.text,
            minimumTemperatureCelsius = minTemperature.degrees,
            maximumTemperatureCelsius = maxTemperature.degrees,
            relativeHumidityPercent = matchPeriod?.relativeHumidity ?: 0,
            rainfallProbabilityPercent = periods.maxOfOrNull {
                it.precipitation?.probability?.percent ?: 0
            } ?: 0,
            rainfallAmountMillimetres = periods.sumOf {
                it.precipitation?.qpf?.takeIf { qpf ->
                    qpf.unit.equals("MILLIMETERS", ignoreCase = true)
                }?.quantity ?: 0.0
            },
            uvIndex = periods.maxOfOrNull { it.uvIndex ?: 0 } ?: 0,
        )
    }
}

@Serializable
private data class ForecastPeriodDto(
    val interval: ForecastIntervalDto,
    val weatherCondition: WeatherConditionDto? = null,
    val relativeHumidity: Int? = null,
    val uvIndex: Int? = null,
    val precipitation: PrecipitationDto? = null,
)

@Serializable
private data class ForecastIntervalDto(
    val startTime: String,
    val endTime: String,
)

private operator fun ForecastIntervalDto.contains(instant: Instant): Boolean =
    instant >= Instant.parse(startTime) && instant < Instant.parse(endTime)

@Serializable
private data class WeatherConditionDto(
    val description: LocalizedTextDto? = null,
)

@Serializable
private data class LocalizedTextDto(
    val text: String,
)

@Serializable
private data class TemperatureDto(
    val degrees: Double,
)

@Serializable
private data class PrecipitationDto(
    val probability: ProbabilityDto? = null,
    val qpf: QuantityDto? = null,
)

@Serializable
private data class ProbabilityDto(
    val percent: Int,
)

@Serializable
private data class QuantityDto(
    val quantity: Double,
    val unit: String,
)
