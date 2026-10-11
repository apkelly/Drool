package com.github.apkelly.drool.schedule.domain.usecase

import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityDraft
import com.github.apkelly.drool.schedule.domain.repository.AvailabilityRepository

class GetRefereeClubsUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(
        profile: LinkedProfile,
    ) = repository.getRefereeClubs(profile)
}

class GetRefereeAvailabilityUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(
        profile: LinkedProfile,
    ) = repository.getAvailability(profile)
}

class CreateRefereeAvailabilityUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(
        profile: LinkedProfile,
        draft: RefereeAvailabilityDraft,
    ) = repository.createAvailability(profile, draft)
}

class DeleteRefereeAvailabilityUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(
        profile: LinkedProfile,
        availabilityId: String,
    ) = repository.deleteAvailability(profile, availabilityId)
}
