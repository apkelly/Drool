package com.github.apkelly.drool.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.schedule.domain.model.AvailabilityFailure
import com.github.apkelly.drool.schedule.domain.model.AvailabilityResult
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityDraft
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityOverview
import com.github.apkelly.drool.schedule.domain.usecase.CreateRefereeAvailabilityUseCase
import com.github.apkelly.drool.schedule.domain.usecase.DeleteRefereeAvailabilityUseCase
import com.github.apkelly.drool.schedule.domain.usecase.GetRefereeAvailabilityUseCase
import com.github.apkelly.drool.schedule.domain.usecase.GetRefereeClubsUseCase
import com.github.apkelly.drool.schedule.ui.model.AvailabilityUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AvailabilityViewModel(
    private val getRefereeClubs: GetRefereeClubsUseCase,
    private val getAvailability: GetRefereeAvailabilityUseCase,
    private val createAvailability: CreateRefereeAvailabilityUseCase,
    private val deleteAvailability: DeleteRefereeAvailabilityUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<AvailabilityUiState>(AvailabilityUiState.Idle)
    val state: StateFlow<AvailabilityUiState> = mutableState.asStateFlow()

    private val mutableRefereeProfileIds = MutableStateFlow<Set<String>>(emptySet())
    val refereeProfileIds: StateFlow<Set<String>> = mutableRefereeProfileIds.asStateFlow()

    private val mutableActiveProfileId = MutableStateFlow<String?>(null)
    val activeProfileId: StateFlow<String?> = mutableActiveProfileId.asStateFlow()

    private val mutableShouldRequestNotificationPermission = MutableStateFlow(false)
    val shouldRequestNotificationPermission: StateFlow<Boolean> =
        mutableShouldRequestNotificationPermission.asStateFlow()

    private val checkedProfileIds = mutableSetOf<String>()
    private val overviewCache = mutableMapOf<String, RefereeAvailabilityOverview>()
    private var linkedProfilesById = emptyMap<String, LinkedProfile>()
    private var configuredAccountId: String? = null
    private var configurationVersion = 0

    fun configureLinkedProfiles(
        accountId: String,
        profiles: List<LinkedProfile>,
    ) {
        if (configuredAccountId != accountId) {
            configuredAccountId = accountId
            configurationVersion += 1
            mutableActiveProfileId.value = null
            checkedProfileIds.clear()
            overviewCache.clear()
            mutableRefereeProfileIds.value = emptySet()
            mutableShouldRequestNotificationPermission.value = false
            mutableState.value = AvailabilityUiState.Idle
        }
        linkedProfilesById = profiles.associateBy(LinkedProfile::id)
        checkEligibility(profiles)
    }

    fun checkEligibility(
        profiles: List<LinkedProfile>,
    ) {
        profiles.distinctBy(LinkedProfile::id).forEach { profile ->
            val profileId = profile.id
            if (!checkedProfileIds.add(profileId)) return@forEach
            val requestVersion = configurationVersion
            viewModelScope.launch {
                when (val result = getRefereeClubs(profile)) {
                    is AvailabilityResult.Success -> {
                        if (
                            configurationVersion == requestVersion &&
                            result.value.isNotEmpty()
                        ) {
                            mutableRefereeProfileIds.update { it + profileId }
                        }
                    }

                    is AvailabilityResult.Failed -> {
                        if (configurationVersion == requestVersion) {
                            checkedProfileIds.remove(profileId)
                        }
                    }
                }
            }
        }
    }

    fun load(
        profileId: String,
        force: Boolean = false,
    ) {
        val profile = linkedProfilesById[profileId]
        if (profile == null) {
            mutableState.value = AvailabilityUiState.Failed(AvailabilityFailure.InvalidResponse)
            return
        }
        mutableActiveProfileId.value = profileId
        overviewCache[profileId]
            ?.takeUnless { force }
            ?.let {
                mutableState.value = it.toReady()
                return
            }
        mutableState.value = AvailabilityUiState.Loading
        val requestVersion = configurationVersion
        viewModelScope.launch {
            when (val result = getAvailability(profile)) {
                is AvailabilityResult.Success -> {
                    if (configurationVersion != requestVersion) return@launch
                    overviewCache[profileId] = result.value
                    if (result.value.clubs.isNotEmpty()) {
                        mutableRefereeProfileIds.update { it + profileId }
                    }
                    if (mutableActiveProfileId.value == profileId) {
                        mutableState.value = result.value.toReady()
                    }
                }

                is AvailabilityResult.Failed -> {
                    if (
                        configurationVersion == requestVersion &&
                        mutableActiveProfileId.value == profileId
                    ) {
                        mutableState.value = AvailabilityUiState.Failed(result.reason)
                    }
                }
            }
        }
    }

    fun create(
        profileId: String,
        draft: RefereeAvailabilityDraft,
    ) {
        val ready = mutableState.value as? AvailabilityUiState.Ready ?: return
        if (ready.isSaving) return
        val profile = linkedProfilesById[profileId]
        if (profile == null) {
            mutableState.value = ready.copy(failure = AvailabilityFailure.InvalidResponse)
            return
        }
        mutableState.value = ready.copy(isSaving = true, failure = null)
        val requestVersion = configurationVersion
        viewModelScope.launch {
            when (val result = createAvailability(profile, draft)) {
                is AvailabilityResult.Success -> {
                    if (configurationVersion != requestVersion) return@launch
                    mutableShouldRequestNotificationPermission.value = true
                    overviewCache.remove(profileId)
                    if (mutableActiveProfileId.value == profileId) {
                        load(profileId, force = true)
                    }
                }

                is AvailabilityResult.Failed -> {
                    if (
                        configurationVersion == requestVersion &&
                        mutableActiveProfileId.value == profileId
                    ) {
                        mutableState.update { current ->
                            (current as? AvailabilityUiState.Ready)?.copy(
                                isSaving = false,
                                failure = result.reason,
                            ) ?: current
                        }
                    }
                }
            }
        }
    }

    fun consumeNotificationPermissionRequest() {
        mutableShouldRequestNotificationPermission.value = false
    }

    fun delete(
        profileId: String,
        availabilityId: String,
    ) {
        val ready = mutableState.value as? AvailabilityUiState.Ready ?: return
        if (ready.deletingAvailabilityId != null) return
        val profile = linkedProfilesById[profileId]
        if (profile == null) {
            mutableState.value = ready.copy(failure = AvailabilityFailure.InvalidResponse)
            return
        }
        mutableState.value = ready.copy(
            deletingAvailabilityId = availabilityId,
            failure = null,
        )
        val requestVersion = configurationVersion
        viewModelScope.launch {
            when (val result = deleteAvailability(profile, availabilityId)) {
                is AvailabilityResult.Success -> {
                    if (configurationVersion != requestVersion) return@launch
                    val updated = ready.availabilities.filterNot { it.id == availabilityId }
                    overviewCache[profileId] = RefereeAvailabilityOverview(
                        clubs = ready.clubs,
                        availabilities = updated,
                    )
                    if (mutableActiveProfileId.value == profileId) {
                        mutableState.value = ready.copy(availabilities = updated)
                    }
                }

                is AvailabilityResult.Failed -> {
                    if (
                        configurationVersion == requestVersion &&
                        mutableActiveProfileId.value == profileId
                    ) {
                        mutableState.update { current ->
                            (current as? AvailabilityUiState.Ready)?.copy(
                                deletingAvailabilityId = null,
                                failure = result.reason,
                            ) ?: current
                        }
                    }
                }
            }
        }
    }

    fun clearFailure() {
        mutableState.update { current ->
            (current as? AvailabilityUiState.Ready)?.copy(failure = null) ?: current
        }
    }

    private fun RefereeAvailabilityOverview.toReady() = AvailabilityUiState.Ready(
        clubs = clubs,
        availabilities = availabilities,
    )
}
