package com.github.apkelly.drool.match.data.weather

import com.github.apkelly.drool.common.domain.model.MatchWeather
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.time.Instant

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
            parameter("days", FORECAST_DAYS)
            parameter("pageSize", FORECAST_DAYS)
            parameter("unitsSystem", "METRIC")
        }.requireWeatherBody<ForecastDaysResponse>(url)
        val kickoff = Instant.fromEpochMilliseconds(kickoffEpochMillis)
        return response.forecastDays
            .firstOrNull { kickoff in it.interval }
            ?.toMatchWeather(kickoff)
    }

    private suspend inline fun <reified T> HttpResponse.requireWeatherBody(
        url: String,
    ): T {
        requireWeatherSuccess(url)
        return try {
            body()
        } catch (error: JsonConvertException) {
            throw WeatherResponseException(url, error)
        } catch (error: SerializationException) {
            throw WeatherResponseException(url, error)
        }
    }

    private suspend fun HttpResponse.requireWeatherSuccess(
        url: String,
    ) {
        if (!status.isSuccess()) {
            throw WeatherHttpException(status.value, url, bodyAsText())
        }
    }

    class WeatherHttpException(
        val statusCode: Int,
        val url: String,
        val responseBody: String,
    ) : IllegalStateException("HTTP $statusCode from $url")

    class WeatherResponseException(
        url: String,
        cause: Throwable,
    ) : IllegalStateException("Response from $url did not match the weather contract", cause)

    private companion object {
        const val FORECAST_DAYS = 10
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
    fun toMatchWeather(
        kickoff: Instant,
    ): MatchWeather {
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

private operator fun ForecastIntervalDto.contains(
    instant: Instant,
): Boolean = instant >= Instant.parse(startTime) && instant < Instant.parse(endTime)

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
