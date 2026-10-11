package com.github.apkelly.drool.profile.ui.model

import com.github.apkelly.drool.common.domain.model.LinkMemberFailure
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.ThemeMode

data class ProfileUiModel(
    val profile: Profile,
    val themeMode: ThemeMode,
    val observabilityEnabled: Boolean,
    val unlinkMemberState: UnlinkMemberUiState = UnlinkMemberUiState.Idle,
)

sealed interface UnlinkMemberUiState {
    data object Idle : UnlinkMemberUiState

    data class Unlinking(
        val linkId: String,
    ) : UnlinkMemberUiState

    data class Failed(
        val reason: LinkMemberFailure,
    ) : UnlinkMemberUiState
}
