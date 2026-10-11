package com.github.apkelly.drool.profile.ui.model

import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.ThemeMode

data class ProfileUiModel(
    val profile: Profile,
    val themeMode: ThemeMode,
    val observabilityEnabled: Boolean,
)
