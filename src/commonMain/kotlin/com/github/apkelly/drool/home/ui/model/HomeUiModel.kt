package com.github.apkelly.drool.home.ui.model

import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.ProfileTeam
import com.github.apkelly.drool.common.domain.model.RefreshResult
import com.github.apkelly.drool.common.ui.model.CollectionUiState

data class HomeUiModel(
    val profile: Profile,
    val linkedProfiles: List<LinkedProfile>,
    val selectedProfileId: String?,
    val profileTeams: List<ProfileTeam>,
    val linkedProfileRefreshResults: Map<String, RefreshResult>,
    val fixtures: CollectionUiState<Fixture>,
)
