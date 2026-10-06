package com.github.apkelly.drool.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.domain.model.Club
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FixtureRole
import com.github.apkelly.drool.domain.model.FamilyProfile
import com.github.apkelly.drool.domain.model.FamilyTeam
import com.github.apkelly.drool.domain.model.FamilyClub
import com.github.apkelly.drool.domain.model.Team
import com.github.apkelly.drool.domain.model.TeamRelationship
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.action_follow
import com.github.apkelly.drool.resources.action_following
import com.github.apkelly.drool.resources.content_club_logo
import com.github.apkelly.drool.resources.fixture_competition
import com.github.apkelly.drool.resources.fixture_venue
import com.github.apkelly.drool.resources.fixture_versus
import com.github.apkelly.drool.resources.fixture_role_player
import com.github.apkelly.drool.resources.fixture_role_referee
import com.github.apkelly.drool.resources.fixture_role_referee_named
import com.github.apkelly.drool.resources.family_all_profiles
import com.github.apkelly.drool.resources.family_profile_item
import com.github.apkelly.drool.resources.team_age_group
import com.github.apkelly.drool.resources.content_team_logo
import com.github.apkelly.drool.resources.team_your_team
import com.github.apkelly.drool.ui.format.formatFixtureDateTime
import com.github.apkelly.drool.ui.icons.WhistleIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun ClubCard(
    club: Club,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brandColor = club.primaryColor.toColorOrNull() ?: MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (brandColor.luminance() > 0.45f) Color.Black else Color.White
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = brandColor,
            contentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteImage(
                url = club.logoUrl,
                contentDescription = stringResource(Res.string.content_club_logo, club.name),
                fallbackIcon = Icons.Default.Groups,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = club.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                club.shortName?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
    }
}
}

@Composable
fun TeamCard(
    team: Team,
    relationship: TeamRelationship,
    onFollowingChanged: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RemoteImage(
                url = team.logoUrl,
                contentDescription = stringResource(Res.string.content_team_logo, team.name),
                fallbackIcon = Icons.Default.Groups,
                modifier = Modifier.size(44.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(team.name, style = MaterialTheme.typography.titleMedium)
                team.ageGroup?.let {
                    Text(
                        stringResource(Res.string.team_age_group, it),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (relationship == TeamRelationship.PlaysFor) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(Res.string.team_your_team)) },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                    )
                }
            }
            if (relationship != TeamRelationship.PlaysFor) {
                val following = relationship == TeamRelationship.Following
                Button(onClick = { onFollowingChanged(!following) }) {
                    Text(
                        stringResource(
                            if (following) Res.string.action_following else Res.string.action_follow
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FixtureCard(
    fixture: Fixture,
    profileName: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
        colors = if (fixture.role == FixtureRole.Referee) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (fixture.role == FixtureRole.Referee) {
                        WhistleIcon
                    } else {
                        Icons.Default.SportsSoccer
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (fixture.role == FixtureRole.Referee) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                Text(
                    text = when {
                        fixture.role == FixtureRole.Referee &&
                            fixture.refereeRole != null -> {
                            stringResource(
                                Res.string.fixture_role_referee_named,
                                fixture.refereeRole,
                            )
                        }
                        fixture.role == FixtureRole.Referee ->
                            stringResource(Res.string.fixture_role_referee)
                        else -> stringResource(Res.string.fixture_role_player)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            profileName?.let {
                Text(
                    text = stringResource(Res.string.family_profile_item, it),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text(
                text = formatFixtureDateTime(fixture.kickoffEpochMillis),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(
                    Res.string.fixture_versus,
                    fixture.homeTeamName,
                    fixture.awayTeamName,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            fixture.competitionName?.let {
                Text(stringResource(Res.string.fixture_competition, it))
            }
            fixture.venueName?.let {
                Text(
                    stringResource(Res.string.fixture_venue, it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun FamilyProfileSelector(
            profiles: List<FamilyProfile>,
            selectedProfileId: String?,
            onSelected: (String?) -> Unit,
            modifier: Modifier = Modifier,
        ) {
            Row(
                modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedProfileId == null,
                    onClick = { onSelected(null) },
                    label = { Text(stringResource(Res.string.family_all_profiles)) },
                )
                profiles.forEach { profile ->
                    FilterChip(
                        selected = selectedProfileId == profile.id,
                        onClick = { onSelected(profile.id) },
                        label = { Text(profile.displayName) },
                        leadingIcon = {
                            RemoteImage(
                                url = profile.avatarUrl,
                                contentDescription = profile.displayName,
                                fallbackIcon = Icons.Default.Groups,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                    )
                }
            }
        }

@Composable
fun FamilyTeamCard(
            familyTeam: FamilyTeam,
            profileName: String,
            modifier: Modifier = Modifier,
            onClick: (() -> Unit)? = null,
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RemoteImage(
                        url = familyTeam.team.logoUrl,
                        contentDescription = stringResource(
                            Res.string.content_team_logo,
                            familyTeam.team.name,
                        ),
                        fallbackIcon = Icons.Default.Groups,
                        modifier = Modifier.size(44.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(familyTeam.team.name, style = MaterialTheme.typography.titleMedium)
                        Text(profileName, style = MaterialTheme.typography.bodySmall)
                    }

                    AssistChip(
                        onClick = {},
                        label = { Text(familyTeam.relationship.name) },
                    )
                }
        }
    }

@Composable
fun FamilyClubCard(
    familyClub: FamilyClub,
    profileName: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RemoteImage(
                url = familyClub.club.logoUrl,
                contentDescription = stringResource(
                    Res.string.content_club_logo,
                    familyClub.club.name,
                ),
                fallbackIcon = Icons.Default.Groups,
                modifier = Modifier.size(44.dp),
            )
            Column {
                Text(familyClub.club.name, style = MaterialTheme.typography.titleMedium)
                Text(profileName, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

internal fun String?.toColorOrNull(): Color? {
    val value = this?.removePrefix("#") ?: return null
    if (value.length != 6 && value.length != 8) return null
    val parsed = value.toULongOrNull(16) ?: return null
    val argb = if (value.length == 6) parsed or 0xFF000000uL else parsed
    return Color(argb.toInt())
}
