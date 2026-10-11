package com.github.apkelly.drool.schedule.domain.usecase

import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityDraft
import com.github.apkelly.drool.schedule.domain.repository.AvailabilityRepository

class GetRefereeClubsUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(profileId: String) =
        repository.getRefereeClubs(profileId)
}

class GetRefereeAvailabilityUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(profileId: String) =
        repository.getAvailability(profileId)
}

class CreateRefereeAvailabilityUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(profileId: String, draft: RefereeAvailabilityDraft) =
        repository.createAvailability(profileId, draft)
}

class DeleteRefereeAvailabilityUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(profileId: String, availabilityId: String) =
        repository.deleteAvailability(profileId, availabilityId)
}
