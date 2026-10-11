package com.github.apkelly.drool.schedule.data

import com.github.apkelly.dribl.api.DriblHttpException
import com.github.apkelly.dribl.model.AvailabilityPeriod
import com.github.apkelly.dribl.model.RefereeAvailability
import com.github.apkelly.dribl.model.RefereeClub
import com.github.apkelly.drool.common.data.repository.FakeSportsRemote
import com.github.apkelly.drool.common.data.repository.TestTokenStore
import com.github.apkelly.drool.common.data.time.TimeProvider
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.schedule.domain.model.AvailabilityFailure
import com.github.apkelly.drool.schedule.domain.model.AvailabilityRecurrence
import com.github.apkelly.drool.schedule.domain.model.AvailabilityResult
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityDraft
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class AvailabilityRepositoryImplTest {
    @Test
    fun loadUsesAnImpersonatedProfileSessionAndMapsAvailability() = runTest {
        TestTokenStore().use { testStore ->
            testStore.store.saveBearerToken("root-token")
            val remote = FakeSportsRemote().apply {
                refereeClubs = listOf(refereeClub)
                refereeAvailabilities = AvailabilityPeriod.entries.map { period ->
                    refereeAvailability.copy(id = period.name, period = period)
                }
            }
            val repository = AvailabilityRepositoryImpl(
                remote,
                testStore.store,
                fixedTimeProvider,
            )

            val clubs = assertIs<
                AvailabilityResult.Success<
                    List<com.github.apkelly.drool.schedule.domain.model.RefereeClub>,
                    >,
                >(
                repository.getRefereeClubs(linkedProfile),
            )
            val result = assertIs<AvailabilityResult.Success<*>>(
                repository.getAvailability(linkedProfile),
            )
            val overview = assertIs<
                com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityOverview,
                >(result.value)

            assertEquals(
                "Marrickville Referees",
                clubs.value.single().name,
            )
            assertEquals(listOf("1001", "1001"), remote.profileSessionUsers)
            assertEquals(
                "1001" to "2026-10-12T00:00:00Z",
                remote.refereeAvailabilityQuery,
            )
            assertEquals("Marrickville Referees", overview.clubs.single().name)
            assertEquals(
                setOf("child"),
                overview.availabilities.map { it.profileId }.toSet(),
            )
            assertEquals(
                AvailabilityRecurrence.entries,
                overview.availabilities.map { it.recurrence },
            )
        }
    }

    @Test
    fun primaryProfileUsesRootSessionAndPersonId() = runTest {
        TestTokenStore().use { testStore ->
            testStore.store.saveBearerToken("root-token")
            val remote = FakeSportsRemote()
            val repository = AvailabilityRepositoryImpl(
                remote,
                testStore.store,
                fixedTimeProvider,
            )
            val primaryProfile = LinkedProfile(
                id = "account",
                displayName = "Harry Kane",
                avatarUrl = null,
                isPrimary = true,
                subjectUserId = "1000",
            )

            val result = repository.getAvailability(primaryProfile)

            assertIs<AvailabilityResult.Success<*>>(result)
            assertEquals(emptyList(), remote.profileSessionUsers)
            assertEquals(
                "1000" to "2026-10-11T00:00:00Z",
                remote.refereeAvailabilityQuery,
            )
        }
    }

    @Test
    fun weeklyCreateBuildsTimezoneCorrectDriblRequest() = runTest {
        TestTokenStore().use { testStore ->
            testStore.store.saveBearerToken("root-token")
            val remote = FakeSportsRemote().apply {
                refereeAvailabilities = listOf(refereeAvailability)
            }
            val repository = AvailabilityRepositoryImpl(
                remote,
                testStore.store,
                fixedTimeProvider,
            )

            val result = repository.createAvailability(
                profile = linkedProfile,
                draft = RefereeAvailabilityDraft(
                    startDateEpochMillis =
                    Instant.parse("2026-10-13T00:00:00Z").toEpochMilliseconds(),
                    endDateEpochMillis =
                    Instant.parse("2026-12-29T00:00:00Z").toEpochMilliseconds(),
                    startHour = 18,
                    startMinute = 0,
                    endHour = 19,
                    endMinute = 0,
                    recurrence = AvailabilityRecurrence.Weekly,
                    clubId = "5129",
                    timeZone = "Australia/Sydney",
                ),
            )

            assertIs<AvailabilityResult.Success<Unit>>(result)
            with(requireNotNull(remote.createAvailabilityRequest)) {
                assertEquals("1001", userId)
                assertEquals("2026-10-13T07:00:00Z", start)
                assertEquals("2026-12-29T08:00:00Z", end)
                assertEquals("18:00", startTime)
                assertEquals("19:00", endTime)
                assertEquals("Weekly", period)
                assertEquals(2, day)
                assertEquals("Australia/Sydney", timeZone)
            }
            listOf(
                AvailabilityRecurrence.Fortnightly,
                AvailabilityRecurrence.Monthly,
            ).forEach { recurrence ->
                repository.createAvailability(
                    profile = linkedProfile,
                    draft = RefereeAvailabilityDraft(
                        startDateEpochMillis =
                        Instant.parse("2026-10-13T00:00:00Z").toEpochMilliseconds(),
                        endDateEpochMillis =
                        Instant.parse("2026-12-29T00:00:00Z").toEpochMilliseconds(),
                        startHour = 18,
                        startMinute = 0,
                        endHour = 19,
                        endMinute = 0,
                        recurrence = recurrence,
                        clubId = "5129",
                        timeZone = "Australia/Sydney",
                    ),
                )
                with(requireNotNull(remote.createAvailabilityRequest)) {
                    assertEquals(recurrence.name, period)
                    assertEquals(2, day)
                }
            }
        }
    }

    @Test
    fun missingTokenAndKnownHttpFailuresMapToDomainFailures() = runTest {
        TestTokenStore().use { testStore ->
            val remote = FakeSportsRemote()
            val repository = AvailabilityRepositoryImpl(
                remote,
                testStore.store,
                fixedTimeProvider,
            )

            assertEquals(
                AvailabilityResult.Failed(AvailabilityFailure.Unauthorized),
                repository.getRefereeClubs(linkedProfile),
            )

            testStore.store.saveBearerToken("root-token")
            remote.failure = DriblHttpException(403, "/availabilities", "forbidden")

            assertEquals(
                AvailabilityResult.Failed(AvailabilityFailure.Forbidden),
                repository.getRefereeClubs(linkedProfile),
            )
        }
    }

    @Test
    fun createRejectsAvailabilityBeforeTodayInTheClubTimezone() = runTest {
        TestTokenStore().use { testStore ->
            testStore.store.saveBearerToken("root-token")
            val remote = FakeSportsRemote()
            val repository = AvailabilityRepositoryImpl(
                remote,
                testStore.store,
                fixedTimeProvider,
            )

            val result = repository.createAvailability(
                profile = linkedProfile,
                draft = RefereeAvailabilityDraft(
                    startDateEpochMillis =
                    Instant.parse("2026-10-11T00:00:00Z").toEpochMilliseconds(),
                    endDateEpochMillis = null,
                    startHour = 18,
                    startMinute = 0,
                    endHour = 19,
                    endMinute = 0,
                    recurrence = AvailabilityRecurrence.SingleDay,
                    clubId = "5129",
                    timeZone = "Australia/Sydney",
                ),
            )

            assertEquals(
                AvailabilityResult.Failed(AvailabilityFailure.InvalidResponse),
                result,
            )
            assertEquals(null, remote.createAvailabilityRequest)
        }
    }

    @Test
    fun createRejectsAvailabilityMoreThanSixCalendarMonthsAhead() = runTest {
        TestTokenStore().use { testStore ->
            testStore.store.saveBearerToken("root-token")
            val remote = FakeSportsRemote()
            val repository = AvailabilityRepositoryImpl(
                remote,
                testStore.store,
                fixedTimeProvider,
            )

            val result = repository.createAvailability(
                profile = linkedProfile,
                draft = RefereeAvailabilityDraft(
                    startDateEpochMillis =
                    Instant.parse("2027-04-13T00:00:00Z").toEpochMilliseconds(),
                    endDateEpochMillis = null,
                    startHour = 18,
                    startMinute = 0,
                    endHour = 19,
                    endMinute = 0,
                    recurrence = AvailabilityRecurrence.SingleDay,
                    clubId = "5129",
                    timeZone = "Australia/Sydney",
                ),
            )

            assertEquals(
                AvailabilityResult.Failed(AvailabilityFailure.InvalidResponse),
                result,
            )
            assertEquals(null, remote.createAvailabilityRequest)
        }
    }

    @Test
    fun loadUsesUtcTodayWhenNoRefereeClubIsReturned() = runTest {
        TestTokenStore().use { testStore ->
            testStore.store.saveBearerToken("root-token")
            val remote = FakeSportsRemote()
            val repository = AvailabilityRepositoryImpl(
                remote,
                testStore.store,
                fixedTimeProvider,
            )

            assertIs<AvailabilityResult.Success<*>>(
                repository.getAvailability(linkedProfile),
            )

            assertEquals(
                "1001" to "2026-10-11T00:00:00Z",
                remote.refereeAvailabilityQuery,
            )
        }
    }

    @Test
    fun deleteUsesTheSelectedAvailabilityId() = runTest {
        TestTokenStore().use { testStore ->
            testStore.store.saveBearerToken("root-token")
            val remote = FakeSportsRemote()
            val repository = AvailabilityRepositoryImpl(
                remote,
                testStore.store,
                fixedTimeProvider,
            )

            val result = repository.deleteAvailability(linkedProfile, "21484775")

            assertIs<AvailabilityResult.Success<Unit>>(result)
            assertEquals("21484775", remote.deletedAvailabilityId)
        }
    }

    private companion object {
        val fixedTimeProvider = TimeProvider {
            Instant.parse("2026-10-11T14:00:00Z").toEpochMilliseconds()
        }
        val linkedProfile = LinkedProfile(
            id = "child",
            displayName = "Jude Bellingham",
            avatarUrl = null,
            isPrimary = false,
            subjectUserId = "1001",
        )
        val refereeClub = RefereeClub(
            id = "5129",
            name = "Marrickville Referees",
            logoUrl = null,
            timeZone = "Australia/Sydney",
        )
        val refereeAvailability = RefereeAvailability(
            id = "21484775",
            startEpochMillis = Instant.parse("2026-10-13T07:00:00Z").toEpochMilliseconds(),
            endEpochMillis = Instant.parse("2026-10-13T08:00:00Z").toEpochMilliseconds(),
            clubId = "5129",
            clubName = "Marrickville Referees",
            period = AvailabilityPeriod.Weekly,
            templateEndEpochMillis =
            Instant.parse("2026-12-29T08:00:00Z").toEpochMilliseconds(),
        )
    }
}
