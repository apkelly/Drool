package com.github.apkelly.drool.schedule.ui.viewmodel

import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.schedule.domain.model.AvailabilityFailure
import com.github.apkelly.drool.schedule.domain.model.AvailabilityRecurrence
import com.github.apkelly.drool.schedule.domain.model.AvailabilityResult
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailability
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityDraft
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityOverview
import com.github.apkelly.drool.schedule.domain.model.RefereeClub
import com.github.apkelly.drool.schedule.domain.repository.AvailabilityRepository
import com.github.apkelly.drool.schedule.domain.usecase.CreateRefereeAvailabilityUseCase
import com.github.apkelly.drool.schedule.domain.usecase.DeleteRefereeAvailabilityUseCase
import com.github.apkelly.drool.schedule.domain.usecase.GetRefereeAvailabilityUseCase
import com.github.apkelly.drool.schedule.domain.usecase.GetRefereeClubsUseCase
import com.github.apkelly.drool.schedule.ui.model.AvailabilityUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AvailabilityViewModelTest {
    @Test
    fun eligibilityAndLoadExposeRefereeAvailability() = runTest {
        val repository = FakeAvailabilityRepository()
        val viewModel = viewModel(repository)

        viewModel.configureLinkedProfiles("account", listOf(playerProfile, refereeProfile))
        advanceUntilIdle()

        assertEquals(setOf("referee"), viewModel.refereeProfileIds.value)

        viewModel.load("referee")
        advanceUntilIdle()

        val ready = assertIs<AvailabilityUiState.Ready>(viewModel.state.value)
        assertEquals("availability", ready.availabilities.single().id)
    }

    @Test
    fun createReloadsAndDeleteRemovesTheSelectedAvailability() = runTest {
        val repository = FakeAvailabilityRepository()
        val viewModel = viewModel(repository)
        viewModel.configureLinkedProfiles("account", listOf(refereeProfile))
        viewModel.load("referee")
        advanceUntilIdle()

        viewModel.create("referee", draft)
        advanceUntilIdle()

        assertEquals(draft, repository.createdDraft)
        assertEquals(2, repository.loadCalls)
        assertEquals(true, viewModel.shouldRequestNotificationPermission.value)
        viewModel.consumeNotificationPermissionRequest()
        assertEquals(false, viewModel.shouldRequestNotificationPermission.value)

        viewModel.delete("referee", "availability")
        advanceUntilIdle()

        assertEquals("availability", repository.deletedId)
        assertEquals(
            emptyList(),
            assertIs<AvailabilityUiState.Ready>(viewModel.state.value).availabilities,
        )
    }

    @Test
    fun operationFailureIsSurfacedAndCanBeCleared() = runTest {
        val repository = FakeAvailabilityRepository()
        val viewModel = viewModel(repository)
        viewModel.configureLinkedProfiles("account", listOf(refereeProfile))
        viewModel.load("referee")
        advanceUntilIdle()
        repository.createResult =
            AvailabilityResult.Failed(AvailabilityFailure.Offline)

        viewModel.create("referee", draft)
        advanceUntilIdle()

        assertEquals(false, viewModel.shouldRequestNotificationPermission.value)
        assertEquals(
            AvailabilityFailure.Offline,
            assertIs<AvailabilityUiState.Ready>(viewModel.state.value).failure,
        )
        viewModel.clearFailure()
        assertEquals(
            null,
            assertIs<AvailabilityUiState.Ready>(viewModel.state.value).failure,
        )
    }

    private fun TestScope.viewModel(
        repository: AvailabilityRepository,
    ) = AvailabilityViewModel(
        getRefereeClubs = GetRefereeClubsUseCase(repository),
        getAvailability = GetRefereeAvailabilityUseCase(repository),
        createAvailability = CreateRefereeAvailabilityUseCase(repository),
        deleteAvailability = DeleteRefereeAvailabilityUseCase(repository),
        scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)),
    )

    private class FakeAvailabilityRepository : AvailabilityRepository {
        var loadCalls = 0
        var createdDraft: RefereeAvailabilityDraft? = null
        var deletedId: String? = null
        var createResult: AvailabilityResult<Unit> = AvailabilityResult.Success(Unit)

        override suspend fun getRefereeClubs(
            profile: LinkedProfile,
        ): AvailabilityResult<List<RefereeClub>> =
            AvailabilityResult.Success(if (profile.id == "referee") listOf(club) else emptyList())

        override suspend fun getAvailability(
            profile: LinkedProfile,
        ): AvailabilityResult<RefereeAvailabilityOverview> {
            loadCalls += 1
            return AvailabilityResult.Success(overview)
        }

        override suspend fun createAvailability(
            profile: LinkedProfile,
            draft: RefereeAvailabilityDraft,
        ): AvailabilityResult<Unit> {
            createdDraft = draft
            return createResult
        }

        override suspend fun deleteAvailability(
            profile: LinkedProfile,
            availabilityId: String,
        ): AvailabilityResult<Unit> {
            deletedId = availabilityId
            return AvailabilityResult.Success(Unit)
        }
    }

    private companion object {
        val playerProfile = LinkedProfile("player", "Player", null, isPrimary = true)
        val refereeProfile = LinkedProfile("referee", "Referee", null, isPrimary = false)
        val club = RefereeClub(
            id = "club",
            name = "Referee Club",
            logoUrl = null,
            timeZone = "Australia/Sydney",
        )
        val availability = RefereeAvailability(
            id = "availability",
            profileId = "referee",
            startEpochMillis = 1_000,
            endEpochMillis = 2_000,
            clubId = "club",
            clubName = "Referee Club",
            recurrence = AvailabilityRecurrence.SingleDay,
            recurrenceEndEpochMillis = null,
        )
        val overview = RefereeAvailabilityOverview(
            clubs = listOf(club),
            availabilities = listOf(availability),
        )
        val draft = RefereeAvailabilityDraft(
            startDateEpochMillis = 1_000,
            endDateEpochMillis = null,
            startHour = 18,
            startMinute = 0,
            endHour = 19,
            endMinute = 0,
            recurrence = AvailabilityRecurrence.SingleDay,
            clubId = "club",
            timeZone = "Australia/Sydney",
        )
    }
}
