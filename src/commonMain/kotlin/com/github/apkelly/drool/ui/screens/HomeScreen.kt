package com.github.apkelly.drool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FamilyProfile
import com.github.apkelly.drool.domain.model.FamilyTeam
import com.github.apkelly.drool.domain.model.FamilyClub
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.RefreshResult
import com.github.apkelly.drool.domain.model.RefreshFailure
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.home_greeting
import com.github.apkelly.drool.resources.home_next_game
import com.github.apkelly.drool.resources.home_no_games
import com.github.apkelly.drool.resources.home_no_games_body
import com.github.apkelly.drool.resources.home_overview
import com.github.apkelly.drool.resources.family_teams
import com.github.apkelly.drool.resources.family_clubs
import com.github.apkelly.drool.resources.family_profile_refresh_failed
import com.github.apkelly.drool.resources.family_profile_session_expired
import com.github.apkelly.drool.resources.action_sign_in
import com.github.apkelly.drool.ui.model.CollectionUiState
import com.github.apkelly.drool.ui.widgets.CacheStatus
import com.github.apkelly.drool.ui.widgets.FixtureCard
import com.github.apkelly.drool.ui.widgets.FamilyProfileSelector
import com.github.apkelly.drool.ui.widgets.FamilyTeamCard
import com.github.apkelly.drool.ui.widgets.FamilyClubCard
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profile: Profile,
    familyProfiles: List<FamilyProfile>,
    selectedProfileId: String?,
    familyTeams: List<FamilyTeam>,
    familyClubs: List<FamilyClub>,
    familyRefreshResults: Map<String, RefreshResult>,
    onProfileSelected: (String?) -> Unit,
    fixtures: CollectionUiState<Fixture>,
    onRefresh: () -> Unit,
    onSignInAgain: () -> Unit,
    onTeamSelected: (FamilyTeam) -> Unit,
) {
    val expiredProfileNames = familyRefreshResults
        .filterValues {
            it is RefreshResult.Failed &&
                (it.reason == RefreshFailure.Unauthorized ||
                    it.reason == RefreshFailure.Forbidden)
        }
        .keys
        .map { profileId ->
            familyProfiles.firstOrNull { it.id == profileId }?.displayName ?: profileId
        }
        .distinct()
        .joinToString()

    PullToRefreshBox(
        isRefreshing = fixtures.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    stringResource(Res.string.home_greeting, profile.displayName),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                Text(
                    stringResource(Res.string.home_overview),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                FamilyProfileSelector(
                    profiles = familyProfiles,
                    selectedProfileId = selectedProfileId,
                    onSelected = onProfileSelected,
                )
            }
            if (expiredProfileNames.isNotEmpty()) {
                item(key = "expired-family-session") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(
                                Res.string.family_profile_session_expired,
                                expiredProfileNames,
                            ),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = onSignInAgain) {
                            Text(stringResource(Res.string.action_sign_in))
                        }
                    }
                }
            }
            familyRefreshResults.forEach { (profileId, result) ->
                if (
                    result is RefreshResult.Failed &&
                    result.reason != RefreshFailure.Unauthorized &&
                    result.reason != RefreshFailure.Forbidden
                ) {
                    item(key = "refresh-failure-$profileId") {
                        Text(
                            text = stringResource(
                                Res.string.family_profile_refresh_failed,
                                familyProfiles.firstOrNull { it.id == profileId }
                                    ?.displayName
                                    ?: profileId,
                            ),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item {
                CacheStatus(
                    isStale = fixtures.isStale,
                    failure = fixtures.refreshFailure,
                    hasContent = fixtures.items.isNotEmpty(),
                    onRetry = onRefresh,
                )
            }
            val next = fixtures.items.firstOrNull()
            if (next == null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(Res.string.home_no_games),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(stringResource(Res.string.home_no_games_body))
                    }
                }
            } else {
                item {
                    Text(
                        stringResource(Res.string.home_next_game),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                item {
                    FixtureCard(
                        next,
                        familyProfiles.profileNameFor(next.profileId, selectedProfileId),
                    )
                }
            }
            if (familyTeams.isNotEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.family_teams),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                items(
                    familyTeams.take(HOME_AFFILIATION_LIMIT),
                    key = { "${it.profileId}:${it.team.id}" },
                ) { familyTeam ->
                    FamilyTeamCard(
                        familyTeam = familyTeam,
                        profileName = familyProfiles
                            .firstOrNull { it.id == familyTeam.profileId }
                            ?.displayName
                            ?: familyTeam.profileId,
                        onClick = { onTeamSelected(familyTeam) },
                    )
                }
            }
            if (familyClubs.isNotEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.family_clubs),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                items(
                    familyClubs.take(HOME_AFFILIATION_LIMIT),
                    key = { "${it.profileId}:${it.club.id}" },
                ) { familyClub ->
                    FamilyClubCard(
                        familyClub = familyClub,
                        profileName = familyProfiles
                            .firstOrNull { it.id == familyClub.profileId }
                            ?.displayName
                            ?: familyClub.profileId,
                    )
                }
            }
        }

    }
}

private const val HOME_AFFILIATION_LIMIT = 3

private fun List<FamilyProfile>.profileNameFor(
    profileId: String?,
    selectedProfileId: String?,
): String? = if (selectedProfileId == null) {
    firstOrNull { it.id == profileId }?.displayName
} else {
    null
}
