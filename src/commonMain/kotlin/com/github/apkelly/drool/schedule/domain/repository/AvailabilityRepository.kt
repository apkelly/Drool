package com.github.apkelly.drool.schedule.domain.repository

import com.github.apkelly.drool.schedule.domain.model.AvailabilityResult
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityDraft
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityOverview
import com.github.apkelly.drool.schedule.domain.model.RefereeClub

interface AvailabilityRepository {
    suspend fun getRefereeClubs(
        profileId: String,
    ): AvailabilityResult<List<RefereeClub>>

    suspend fun getAvailability(
        profileId: String,
    ): AvailabilityResult<RefereeAvailabilityOverview>

    suspend fun createAvailability(
        profileId: String,
        draft: RefereeAvailabilityDraft,
    ): AvailabilityResult<Unit>

    suspend fun deleteAvailability(
        profileId: String,
        availabilityId: String,
    ): AvailabilityResult<Unit>
}
