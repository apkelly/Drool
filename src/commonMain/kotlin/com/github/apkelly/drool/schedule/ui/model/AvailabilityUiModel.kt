package com.github.apkelly.drool.schedule.ui.model

import com.github.apkelly.drool.schedule.domain.model.AvailabilityFailure
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailability
import com.github.apkelly.drool.schedule.domain.model.RefereeClub

data class AvailabilityUiModel(
    val profileId: String,
    val profileName: String,
    val state: AvailabilityUiState,
)

sealed interface AvailabilityUiState {
    data object Idle : AvailabilityUiState
    data object Loading : AvailabilityUiState

    data class Ready(
        val clubs: List<RefereeClub>,
        val availabilities: List<RefereeAvailability>,
        val isSaving: Boolean = false,
        val deletingAvailabilityId: String? = null,
        val failure: AvailabilityFailure? = null,
    ) : AvailabilityUiState

    data class Failed(
        val reason: AvailabilityFailure,
    ) : AvailabilityUiState
}
