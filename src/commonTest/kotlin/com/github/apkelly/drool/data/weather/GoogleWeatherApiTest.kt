package com.github.apkelly.drool.data.weather

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.github.apkelly.drool.data.remote.networkJson

class GoogleWeatherApiTest {
    @Test
    fun temperatureUnitsConvertCelsiusForDisplay() {
        assertEquals(20.0, WeatherTemperatureUnit.Celsius.fromCelsius(20.0))
        assertEquals(68.0, WeatherTemperatureUnit.Fahrenheit.fromCelsius(20.0))
        assertEquals("°C", WeatherTemperatureUnit.Celsius.symbol)
        assertEquals("°F", WeatherTemperatureUnit.Fahrenheit.symbol)
    }

    @Test
    fun dailyForecastMapsMatchPeriodAndWholeDayRainfall() = runTest {
        val client = weatherClient { request ->
            assertEquals("api-key", request.url.parameters["key"])
            assertEquals("-33.9", request.url.parameters["location.latitude"])
            assertEquals("151.17", request.url.parameters["location.longitude"])
            assertEquals("10", request.url.parameters["days"])
            assertEquals("10", request.url.parameters["pageSize"])
            assertEquals("METRIC", request.url.parameters["unitsSystem"])
            assertEquals("com.example.test", request.headers["X-Android-Package"])
            forecastResponse
        }

        val weather = GoogleWeatherApi(
            client = client,
            baseUrl = "https://example.test",
            requestHeaders = mapOf("X-Android-Package" to "com.example.test"),
        )
            .fetchMatchWeather(
                apiKey = "api-key",
                latitude = -33.9,
                longitude = 151.17,
                kickoffEpochMillis = 1_791_331_200_000L,
            )

        requireNotNull(weather)
        assertEquals("Showers", weather.condition)
        assertEquals(12.5, weather.minimumTemperatureCelsius)
        assertEquals(22.4, weather.maximumTemperatureCelsius)
        assertEquals(72, weather.relativeHumidityPercent)
        assertEquals(80, weather.rainfallProbabilityPercent)
        assertEquals(13.2, weather.rainfallAmountMillimetres)
        assertEquals(5, weather.uvIndex)
        assertEquals(true, weather.hasElevatedWashoutRisk)
        assertEquals(true, weather.sunscreenRecommended)
    }

    @Test
    fun forecastOutsideReturnedIntervalsIsUnavailable() = runTest {
        val client = weatherClient { forecastResponse }

        val weather = GoogleWeatherApi(client, "https://example.test")
            .fetchMatchWeather("api-key", -33.9, 151.17, 0L)

        assertNull(weather)
    }

    private fun weatherClient(response: (io.ktor.client.request.HttpRequestData) -> String) =
        HttpClient(MockEngine) {
            install(ContentNegotiation) { json(networkJson) }
            engine {
                addHandler { request ->
                    respond(
                        content = response(request),
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }

    private companion object {
        val forecastResponse = """
            {
              "forecastDays": [{
                "interval": {
                  "startTime": "2026-10-06T13:00:00Z",
                  "endTime": "2026-10-07T13:00:00Z"
                },
                "daytimeForecast": {
                  "interval": {
                    "startTime": "2026-10-06T20:00:00Z",
                    "endTime": "2026-10-07T08:00:00Z"
                  },
                  "weatherCondition": {
                    "description": {"text": "Showers", "languageCode": "en"}
                  },
                  "relativeHumidity": 72,
                  "uvIndex": 5,
                  "precipitation": {
                    "probability": {"percent": 80, "type": "RAIN"},
                    "qpf": {"quantity": 11.7, "unit": "MILLIMETERS"}
                  }
                },
                "nighttimeForecast": {
                  "interval": {
                    "startTime": "2026-10-07T08:00:00Z",
                    "endTime": "2026-10-07T13:00:00Z"
                  },
                  "relativeHumidity": 88,
                  "uvIndex": 0,
                  "precipitation": {
                    "probability": {"percent": 40, "type": "RAIN"},
                    "qpf": {"quantity": 1.5, "unit": "MILLIMETERS"}
                  }
                },
                "maxTemperature": {"degrees": 22.4, "unit": "CELSIUS"},
                "minTemperature": {"degrees": 12.5, "unit": "CELSIUS"}
              }]
            }
        """.trimIndent()
    }
}
