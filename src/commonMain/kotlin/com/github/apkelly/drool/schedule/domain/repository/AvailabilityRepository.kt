package com.github.apkelly.drool.schedule.domain.repository

import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.schedule.domain.model.AvailabilityResult
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityDraft
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityOverview
import com.github.apkelly.drool.schedule.domain.model.RefereeClub

interface AvailabilityRepository {
    suspend fun getRefereeClubs(
        profile: LinkedProfile,
    ): AvailabilityResult<List<RefereeClub>>

    suspend fun getAvailability(
        profile: LinkedProfile,
    ): AvailabilityResult<RefereeAvailabilityOverview>

    suspend fun createAvailability(
        profile: LinkedProfile,
        draft: RefereeAvailabilityDraft,
    ): AvailabilityResult<Unit>

    suspend fun deleteAvailability(
        profile: LinkedProfile,
        availabilityId: String,
    ): AvailabilityResult<Unit>
}
