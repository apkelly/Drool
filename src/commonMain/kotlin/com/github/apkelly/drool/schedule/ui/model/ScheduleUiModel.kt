package com.github.apkelly.drool.schedule.ui.model

import com.github.apkelly.drool.domain.model.FamilyProfile
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.ui.model.CollectionUiState

data class ScheduleUiModel(
    val fixtures: CollectionUiState<Fixture>,
    val familyProfiles: List<FamilyProfile>,
    val selectedProfileId: String?,
)
