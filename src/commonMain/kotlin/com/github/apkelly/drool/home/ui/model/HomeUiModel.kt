package com.github.apkelly.drool.home.ui.model

import com.github.apkelly.drool.common.domain.model.FamilyProfile
import com.github.apkelly.drool.common.domain.model.FamilyTeam
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.ui.model.CollectionUiState

data class HomeUiModel(
    val profile: Profile,
    val familyProfiles: List<FamilyProfile>,
    val selectedProfileId: String?,
    val familyTeams: List<FamilyTeam>,
    val familyRefreshResults: Map<String, RefreshResult>,
    val fixtures: CollectionUiState<Fixture>,
)
