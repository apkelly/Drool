package com.github.apkelly.drool.schedule.ui.viewmodel

import com.github.apkelly.drool.schedule.domain.model.AvailabilityResult
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityDraft
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityOverview
import com.github.apkelly.drool.schedule.domain.usecase.CreateRefereeAvailabilityUseCase
import com.github.apkelly.drool.schedule.domain.usecase.DeleteRefereeAvailabilityUseCase
import com.github.apkelly.drool.schedule.domain.usecase.GetRefereeAvailabilityUseCase
import com.github.apkelly.drool.schedule.domain.usecase.GetRefereeClubsUseCase
import com.github.apkelly.drool.schedule.ui.model.AvailabilityUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val mutableState = MutableStateFlow<AvailabilityUiState>(AvailabilityUiState.Idle)
    val state: StateFlow<AvailabilityUiState> = mutableState.asStateFlow()

    private val mutableRefereeProfileIds = MutableStateFlow<Set<String>>(emptySet())
    val refereeProfileIds: StateFlow<Set<String>> = mutableRefereeProfileIds.asStateFlow()

    private val mutableActiveProfileId = MutableStateFlow<String?>(null)
    val activeProfileId: StateFlow<String?> = mutableActiveProfileId.asStateFlow()

    private val checkedProfileIds = mutableSetOf<String>()
    private val overviewCache = mutableMapOf<String, RefereeAvailabilityOverview>()
    private var configuredAccountId: String? = null
    private var configurationVersion = 0

    fun configureFamily(accountId: String, profileIds: List<String>) {
        if (configuredAccountId != accountId) {
            configuredAccountId = accountId
            configurationVersion += 1
            mutableActiveProfileId.value = null
            checkedProfileIds.clear()
            overviewCache.clear()
            mutableRefereeProfileIds.value = emptySet()
            mutableState.value = AvailabilityUiState.Idle
        }
        checkEligibility(profileIds)
    }

    fun checkEligibility(profileIds: List<String>) {
        profileIds.distinct().forEach { profileId ->
            if (!checkedProfileIds.add(profileId)) return@forEach
            val requestVersion = configurationVersion
            scope.launch {
                when (val result = getRefereeClubs(profileId)) {
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

    fun load(profileId: String, force: Boolean = false) {
        mutableActiveProfileId.value = profileId
        overviewCache[profileId]
            ?.takeUnless { force }
            ?.let {
                mutableState.value = it.toReady()
                return
            }
        mutableState.value = AvailabilityUiState.Loading
        val requestVersion = configurationVersion
        scope.launch {
            when (val result = getAvailability(profileId)) {
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

    fun create(profileId: String, draft: RefereeAvailabilityDraft) {
        val ready = mutableState.value as? AvailabilityUiState.Ready ?: return
        if (ready.isSaving) return
        mutableState.value = ready.copy(isSaving = true, failure = null)
        val requestVersion = configurationVersion
        scope.launch {
            when (val result = createAvailability(profileId, draft)) {
                is AvailabilityResult.Success -> {
                    if (configurationVersion != requestVersion) return@launch
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

    fun delete(profileId: String, availabilityId: String) {
        val ready = mutableState.value as? AvailabilityUiState.Ready ?: return
        if (ready.deletingAvailabilityId != null) return
        mutableState.value = ready.copy(
            deletingAvailabilityId = availabilityId,
            failure = null,
        )
        val requestVersion = configurationVersion
        scope.launch {
            when (val result = deleteAvailability(profileId, availabilityId)) {
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
                        mutableState.value = ready.copy(failure = result.reason)
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

    private fun RefereeAvailabilityOverview.toReady() =
        AvailabilityUiState.Ready(
            clubs = clubs,
            availabilities = availabilities,
        )
}
