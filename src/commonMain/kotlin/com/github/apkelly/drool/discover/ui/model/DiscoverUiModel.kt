package com.github.apkelly.drool.discover.ui.model

import com.github.apkelly.drool.domain.model.Club
import com.github.apkelly.drool.ui.model.CollectionUiState

data class DiscoverUiModel(
    val clubs: CollectionUiState<Club>,
)
