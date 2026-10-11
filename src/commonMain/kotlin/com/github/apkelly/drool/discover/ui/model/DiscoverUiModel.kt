package com.github.apkelly.drool.discover.ui.model

import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.ui.model.CollectionUiState

data class DiscoverUiModel(
    val clubs: CollectionUiState<Club>,
)
