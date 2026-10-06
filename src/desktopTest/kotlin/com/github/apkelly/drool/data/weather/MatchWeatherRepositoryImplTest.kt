package com.github.apkelly.drool.data.weather

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.github.apkelly.drool.data.repository.TestDatabase
import com.github.apkelly.drool.data.time.TimeProvider
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FixtureStatus
import com.github.apkelly.drool.domain.model.MatchWeather

class MatchWeatherRepositoryImplTest {
    @Test
    fun cachesAvailableAndUnavailableForecastsForTwentyFourHours() = runTest {
        TestDatabase().use { testDatabase ->
            val remote = FakeWeatherRemote()
            val clock = MutableWeatherTimeProvider()
            val repository = MatchWeatherRepositoryImpl(
                api = remote,
                database = testDatabase.database,
                timeProvider = clock,
                apiKey = "key",
            )
            val fixture = fixture()

            assertEquals(remote.weather, repository.loadMatchWeather(fixture))
            assertEquals(1, remote.calls)

            remote.weather = null
            clock.now = DayMillis - 1
            assertEquals("Fine", repository.loadMatchWeather(fixture)?.condition)
            assertEquals(1, remote.calls)

            clock.now = DayMillis
            assertNull(repository.loadMatchWeather(fixture))
            assertEquals(2, remote.calls)

            clock.now += DayMillis - 1
            assertNull(repository.loadMatchWeather(fixture))
            assertEquals(2, remote.calls)
        }
    }

    @Test
    fun missingCoordinatesOrApiKeyDoesNotRequestWeather() = runTest {
        TestDatabase().use { testDatabase ->
            val remote = FakeWeatherRemote()
            val withoutKey = MatchWeatherRepositoryImpl(
                remote,
                testDatabase.database,
                MutableWeatherTimeProvider(),
                apiKey = null,
            )
            assertNull(withoutKey.loadMatchWeather(fixture()))

            val withKey = MatchWeatherRepositoryImpl(
                remote,
                testDatabase.database,
                MutableWeatherTimeProvider(),
                apiKey = "key",
            )
            assertNull(withKey.loadMatchWeather(fixture().copy(latitude = null)))
            assertNull(withKey.loadMatchWeather(fixture().copy(longitude = null)))
            assertEquals(0, remote.calls)
        }
    }

    private fun fixture() = Fixture(
        id = "match",
        kickoffEpochMillis = 1_791_331_200_000L,
        homeTeamId = "home",
        homeTeamName = "Home",
        awayTeamId = "away",
        awayTeamName = "Away",
        competitionName = "League",
        venueName = "Oval",
        userTeamId = "home",
        status = FixtureStatus.Scheduled,
        latitude = -33.9,
        longitude = 151.17,
    )

    private companion object {
        const val DayMillis = 24L * 60L * 60L * 1_000L
    }
}

private class FakeWeatherRemote : WeatherRemoteDataSource {
    var weather: MatchWeather? = MatchWeather("Fine", 15.0, 25.0, 50, 10, 0.0, 7)
    var calls = 0

    override suspend fun fetchMatchWeather(
        apiKey: String,
        latitude: Double,
        longitude: Double,
        kickoffEpochMillis: Long,
    ): MatchWeather? {
        calls += 1
        return weather
    }
}

private class MutableWeatherTimeProvider(var now: Long = 0L) : TimeProvider {
    override fun nowEpochMillis(): Long = now
}
