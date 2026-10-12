package com.github.apkelly.drool.common.ui.widgets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.Club
import com.github.apkelly.drool.common.domain.model.Fixture
import com.github.apkelly.drool.common.domain.model.FixtureRole
import com.github.apkelly.drool.common.domain.model.LinkedProfile
import com.github.apkelly.drool.common.domain.model.ProfileTeam
import com.github.apkelly.drool.common.domain.model.Team
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_follow
import com.github.apkelly.drool.common.resources.action_following
import com.github.apkelly.drool.common.resources.content_club_logo
import com.github.apkelly.drool.common.resources.content_team_logo
import com.github.apkelly.drool.common.resources.fixture_competition
import com.github.apkelly.drool.common.resources.fixture_role_coach
import com.github.apkelly.drool.common.resources.fixture_role_player
import com.github.apkelly.drool.common.resources.fixture_role_referee
import com.github.apkelly.drool.common.resources.fixture_role_referee_named
import com.github.apkelly.drool.common.resources.fixture_venue
import com.github.apkelly.drool.common.resources.fixture_versus
import com.github.apkelly.drool.common.resources.fixture_versus_separator
import com.github.apkelly.drool.common.resources.linked_profile_item
import com.github.apkelly.drool.common.resources.linked_profiles_all
import com.github.apkelly.drool.common.resources.match_score
import com.github.apkelly.drool.common.resources.team_age_group
import com.github.apkelly.drool.common.resources.team_relationship_coach
import com.github.apkelly.drool.common.resources.team_relationship_following
import com.github.apkelly.drool.common.resources.team_relationship_player
import com.github.apkelly.drool.common.resources.team_your_team
import com.github.apkelly.drool.common.ui.format.displayTeamNames
import com.github.apkelly.drool.common.ui.format.formatFixtureDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ClubCard(
    club: Club,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brandColor = club.primaryColor.toColorOrNull() ?: MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (brandColor.luminance() > 0.45f) Color.Black else Color.White
    SportsCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = brandColor,
            contentColor = contentColor,
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteImage(
                url = club.logoUrl,
                contentDescription = stringResource(Res.string.content_club_logo, club.name),
                fallbackIcon = MaterialSymbol.Groups,
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
    onFollowingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    SportsCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RemoteImage(
                url = team.logoUrl,
                contentDescription = stringResource(Res.string.content_team_logo, team.name),
                fallbackIcon = MaterialSymbol.Groups,
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
                if (
                    relationship == TeamRelationship.Player ||
                    relationship == TeamRelationship.Coaching
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                stringResource(
                                    if (relationship == TeamRelationship.Coaching) {
                                        Res.string.team_relationship_coach
                                    } else {
                                        Res.string.team_your_team
                                    },
                                ),
                            )
                        },
                        leadingIcon = {
                            MaterialSymbolIcon(
                                MaterialSymbol.Check,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
            if (
                relationship == TeamRelationship.None ||
                relationship == TeamRelationship.Following
            ) {
                val following = relationship == TeamRelationship.Following
                Button(onClick = { onFollowingChange(!following) }) {
                    Text(
                        stringResource(
                            if (following) Res.string.action_following else Res.string.action_follow,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun FixtureCard(
    fixture: Fixture,
    modifier: Modifier = Modifier,
    profileName: String? = null,
    stackedTeams: Boolean = false,
    onClick: (() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val teamNames = fixture.displayTeamNames()
    SportsCard(
        modifier = modifier.fillMaxWidth(),
        colors = if (fixture.role == FixtureRole.Referee) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MaterialSymbolIcon(
                    symbol = when (fixture.role) {
                        FixtureRole.Referee -> MaterialSymbol.Sports
                        FixtureRole.Player -> MaterialSymbol.SportsSoccer
                        FixtureRole.Coach -> MaterialSymbol.Assignment
                    },
                    contentDescription = null,
                    size = 20.dp,
                    tint = when (fixture.role) {
                        FixtureRole.Referee -> MaterialTheme.colorScheme.tertiary
                        FixtureRole.Player -> MaterialTheme.colorScheme.primary
                        FixtureRole.Coach -> MaterialTheme.colorScheme.secondary
                    },
                )
                Text(
                    text = when (fixture.role) {
                        FixtureRole.Referee -> if (fixture.refereeRole != null) {
                            stringResource(
                                Res.string.fixture_role_referee_named,
                                fixture.refereeRole,
                            )
                        } else {
                            stringResource(Res.string.fixture_role_referee)
                        }

                        FixtureRole.Player -> stringResource(Res.string.fixture_role_player)

                        FixtureRole.Coach -> stringResource(Res.string.fixture_role_coach)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            profileName?.let {
                Text(
                    text = stringResource(Res.string.linked_profile_item, it),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text(
                text = formatFixtureDateTime(fixture.kickoffEpochMillis),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RemoteImage(
                    url = fixture.homeTeamLogoUrl,
                    contentDescription = stringResource(
                        Res.string.content_team_logo,
                        fixture.homeTeamName,
                    ),
                    fallbackIcon = MaterialSymbol.Groups,
                    modifier = Modifier.size(44.dp),
                )
                if (fixture.homeScore != null && fixture.awayScore != null) {
                    Text(
                        text = stringResource(
                            Res.string.match_score,
                            fixture.homeScore,
                            fixture.awayScore,
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                } else if (stackedTeams) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = teamNames.home,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(Res.string.fixture_versus_separator),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = teamNames.away,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(
                            Res.string.fixture_versus,
                            teamNames.home,
                            teamNames.away,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
                RemoteImage(
                    url = fixture.awayTeamLogoUrl,
                    contentDescription = stringResource(
                        Res.string.content_team_logo,
                        fixture.awayTeamName,
                    ),
                    fallbackIcon = MaterialSymbol.Groups,
                    modifier = Modifier.size(44.dp),
                )
            }
            fixture.competitionName?.let {
                Text(stringResource(Res.string.fixture_competition, it))
            }
            fixture.venueName?.let {
                Text(
                    stringResource(Res.string.fixture_venue, it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            footer?.invoke(this)
        }
    }
}

@Composable
fun LinkedProfileSelector(
    profiles: List<LinkedProfile>,
    selectedProfileId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedProfileId == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(Res.string.linked_profiles_all)) },
        )
        profiles.forEach { profile ->
            FilterChip(
                selected = selectedProfileId == profile.id,
                onClick = { onSelect(profile.id) },
                label = { Text(profile.displayName) },
                leadingIcon = {
                    RemoteImage(
                        url = profile.avatarUrl,
                        contentDescription = profile.displayName,
                        fallbackIcon = MaterialSymbol.Groups,
                        modifier = Modifier.size(24.dp),
                    )
                },
            )
        }
    }
}

@Composable
fun ProfileTeamCard(
    profileTeam: ProfileTeam,
    profileName: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    SportsCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileTeamIdentity(
                    profileTeam = profileTeam,
                    profileName = profileName,
                    modifier = Modifier.weight(1f),
                    relationshipLabel = profileTeam.relationship.labelResource(),
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                ProfileTeamIdentity(
                    profileTeam = profileTeam,
                    profileName = profileName,
                    modifier = Modifier.fillMaxWidth(),
                )
                profileTeam.relationship.labelResource()?.let { label ->
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(label)) },
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileTeamIdentity(
    profileTeam: ProfileTeam,
    profileName: String,
    modifier: Modifier = Modifier,
    relationshipLabel: StringResource? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RemoteImage(
            url = profileTeam.team.logoUrl,
            contentDescription = stringResource(
                Res.string.content_team_logo,
                profileTeam.team.name,
            ),
            fallbackIcon = MaterialSymbol.Groups,
            modifier = Modifier.size(44.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profileTeam.team.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(profileName, style = MaterialTheme.typography.bodySmall)
            relationshipLabel?.let { label ->
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(label)) },
                )
            }
        }
    }
}

@Composable
private fun SportsCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick == null) {
        Card(
            modifier = modifier,
            colors = colors,
            content = content,
        )
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            colors = colors,
            content = content,
        )
    }
}

private fun TeamRelationship.labelResource() = when (this) {
    TeamRelationship.None -> null
    TeamRelationship.Following -> Res.string.team_relationship_following
    TeamRelationship.Player -> Res.string.team_relationship_player
    TeamRelationship.Coaching -> Res.string.team_relationship_coach
}

/**
 * Parses an API-provided RGB or ARGB hexadecimal color.
 *
 * Invalid branding values return `null` so callers can retain accessible theme defaults. Six
 * digit RGB values are made fully opaque because Compose colors include an explicit alpha channel.
 */
internal fun String?.toColorOrNull(): Color? {
    val value = this?.removePrefix("#") ?: return null
    if (value.length != RGB_HEX_LENGTH && value.length != ARGB_HEX_LENGTH) return null
    val parsed = value.toULongOrNull(16) ?: return null
    val argb = if (value.length == RGB_HEX_LENGTH) parsed or OPAQUE_ALPHA_MASK else parsed
    return Color(argb.toInt())
}

private const val RGB_HEX_LENGTH = 6
private const val ARGB_HEX_LENGTH = 8
private const val OPAQUE_ALPHA_MASK = 0xFF000000uL
