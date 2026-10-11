package com.github.apkelly.drool.schedule.ui.model

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.common.ui.model.CollectionUiState

data class ScheduleUiModel(
    val fixtures: CollectionUiState<Fixture>,
    val linkedProfiles: List<LinkedProfile>,
    val selectedProfileId: String?,
    val canManageRefereeAvailability: Boolean = false,
)
