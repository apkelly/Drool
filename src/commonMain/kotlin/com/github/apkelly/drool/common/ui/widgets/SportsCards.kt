package com.github.apkelly.drool.common.ui.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
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
import com.github.apkelly.drool.common.resources.fixture_versus_separator
import com.github.apkelly.drool.common.resources.linked_profile_item
import com.github.apkelly.drool.common.resources.linked_profiles_all
import com.github.apkelly.drool.common.resources.match_score
import com.github.apkelly.drool.common.resources.team_age_group
import com.github.apkelly.drool.common.resources.team_relationship_coach
import com.github.apkelly.drool.common.resources.team_relationship_following
import com.github.apkelly.drool.common.resources.team_relationship_manager
import com.github.apkelly.drool.common.resources.team_relationship_player
import com.github.apkelly.drool.common.resources.team_your_team
import com.github.apkelly.drool.common.ui.format.displayTeamNames
import com.github.apkelly.drool.common.ui.format.formatFixtureDateTime
import com.github.apkelly.drool.common.ui.platform.rememberLocationActionLauncher
import com.github.apkelly.drool.common.ui.theme.DroolThemeDefaults
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ClubCard(
    club: Club,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brandColor = club.primaryColor.toColorOrNull()
    val contentColor = brandColor?.let {
        if (it.luminance() > LUMINANCE_THRESHOLD) Color.Black else Color.White
    }
    SportsCard(
        modifier = modifier,
        containerColor = brandColor,
        contentColor = contentColor,
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
                modifier = Modifier.size(54.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = club.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                club.shortName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                    )
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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RemoteImage(
                url = team.logoUrl,
                contentDescription = stringResource(Res.string.content_team_logo, team.name),
                fallbackIcon = MaterialSymbol.Groups,
                modifier = Modifier.size(48.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    team.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                team.ageGroup?.let {
                    Text(
                        stringResource(Res.string.team_age_group, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (
                    relationship == TeamRelationship.Player ||
                    relationship == TeamRelationship.Coaching ||
                    relationship == TeamRelationship.Managing
                ) {
                    RoleBadge(
                        text = stringResource(
                            when (relationship) {
                                TeamRelationship.Coaching -> Res.string.team_relationship_coach
                                TeamRelationship.Managing -> Res.string.team_relationship_manager
                                else -> Res.string.team_your_team
                            },
                        ),
                        leadingSymbol = MaterialSymbol.Check,
                    )
                }
            }
            if (
                relationship == TeamRelationship.None ||
                relationship == TeamRelationship.Following
            ) {
                val following = relationship == TeamRelationship.Following
                if (following) {
                    OutlinedButton(onClick = { onFollowingChange(false) }) {
                        Text(stringResource(Res.string.action_following))
                    }
                } else {
                    Button(onClick = { onFollowingChange(true) }) {
                        Text(stringResource(Res.string.action_follow))
                    }
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
    val isReferee = fixture.role == FixtureRole.Referee
    val locationLauncher = rememberLocationActionLauncher()
    val roleLabel = when (fixture.role) {
        FixtureRole.Referee -> if (fixture.refereeRole != null) {
            stringResource(Res.string.fixture_role_referee_named, fixture.refereeRole)
        } else {
            stringResource(Res.string.fixture_role_referee)
        }

        FixtureRole.Player -> stringResource(Res.string.fixture_role_player)

        FixtureRole.Coach -> stringResource(Res.string.fixture_role_coach)
    }
    val badgeTitle = if (profileName != null) {
        "${stringResource(Res.string.linked_profile_item, profileName)} · $roleLabel"
    } else {
        roleLabel
    }

    SportsCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = if (isReferee) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            null
        },
        contentColor = if (isReferee) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            null
        },
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Top bar: Child / Role pill on left, bold Kick-off Date/Time pill on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isReferee) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MaterialSymbolIcon(
                            symbol = when (fixture.role) {
                                FixtureRole.Referee -> MaterialSymbol.Sports
                                FixtureRole.Player -> MaterialSymbol.SportsSoccer
                                FixtureRole.Coach -> MaterialSymbol.Assignment
                            },
                            contentDescription = null,
                            size = 16.dp,
                            tint = when (fixture.role) {
                                FixtureRole.Referee -> MaterialTheme.colorScheme.tertiary
                                FixtureRole.Player -> MaterialTheme.colorScheme.primary
                                FixtureRole.Coach -> MaterialTheme.colorScheme.secondary
                            },
                        )
                        Text(
                            text = badgeTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = if (isReferee) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isReferee) {
                        MaterialTheme.colorScheme.onTertiary
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                ) {
                    Text(
                        text = formatFixtureDateTime(fixture.kickoffEpochMillis),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            if (isReferee) {
                RefereeStatusPill(fixture = fixture)
            }

            // Center matchup block: Team crests, bold team names, and Score or VS badge
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
                    modifier = Modifier.size(48.dp),
                )
                FixtureMatchupCenter(
                    homeTeamName = teamNames.home,
                    awayTeamName = teamNames.away,
                    homeScore = fixture.homeScore,
                    awayScore = fixture.awayScore,
                    stackedTeams = stackedTeams,
                    isReferee = isReferee,
                    modifier = Modifier.weight(1f),
                )
                RemoteImage(
                    url = fixture.awayTeamLogoUrl,
                    contentDescription = stringResource(
                        Res.string.content_team_logo,
                        fixture.awayTeamName,
                    ),
                    fallbackIcon = MaterialSymbol.Groups,
                    modifier = Modifier.size(48.dp),
                )
            }

            // Bottom metadata: Competition and one-tap Venue / Directions pill
            if (fixture.competitionName != null || fixture.venueName != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    fixture.competitionName?.let { competition ->
                        Text(
                            text = stringResource(Res.string.fixture_competition, competition),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isReferee) {
                                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    fixture.venueName?.let { venue ->
                        val address = fixture.venueAddress ?: venue
                        Surface(
                            onClick = {
                                locationLauncher.navigate(
                                    fixture.latitude,
                                    fixture.longitude,
                                    address,
                                )
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(
                                alpha = 0.72f,
                            ),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                MaterialSymbolIcon(
                                    symbol = MaterialSymbol.LocationOn,
                                    contentDescription = null,
                                    size = 15.dp,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = venue,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            footer?.invoke(this)
        }
    }
}

@Composable
private fun FixtureMatchupCenter(
    homeTeamName: String,
    awayTeamName: String,
    homeScore: Int?,
    awayScore: Int?,
    stackedTeams: Boolean,
    isReferee: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = homeTeamName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (homeScore != null && awayScore != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    text = stringResource(Res.string.match_score, homeScore, awayScore),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
        } else {
            Text(
                text = stringResource(Res.string.fixture_versus_separator).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isReferee) {
                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = if (stackedTeams) Modifier else Modifier.padding(vertical = 1.dp),
            )
        }
        Text(
            text = awayTeamName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FamilyProfilePill(
            label = stringResource(Res.string.linked_profiles_all),
            selected = selectedProfileId == null,
            onClick = { onSelect(null) },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (selectedProfileId == null) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    MaterialSymbolIcon(
                        symbol = MaterialSymbol.Groups,
                        contentDescription = null,
                        size = 18.dp,
                        tint = if (selectedProfileId == null) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                }
            },
        )
        profiles.forEach { profile ->
            val isSelected = selectedProfileId == profile.id
            FamilyProfilePill(
                label = profile.displayName,
                selected = isSelected,
                onClick = { onSelect(profile.id) },
                leadingContent = {
                    RemoteImage(
                        url = profile.avatarUrl,
                        contentDescription = profile.displayName,
                        fallbackIcon = MaterialSymbol.Person,
                        modifier = Modifier.size(28.dp).clip(CircleShape),
                        shape = CircleShape,
                        contentScale = ContentScale.Crop,
                    )
                },
            )
        }
    }
}

@Composable
private fun FamilyProfilePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingContent: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = if (selected) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            leadingContent()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
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
                    RoleBadge(
                        text = stringResource(label),
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
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RemoteImage(
            url = profileTeam.team.logoUrl,
            contentDescription = stringResource(
                Res.string.content_team_logo,
                profileTeam.team.name,
            ),
            fallbackIcon = MaterialSymbol.Groups,
            modifier = Modifier.size(48.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = profileTeam.team.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = profileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            relationshipLabel?.let { label ->
                Spacer(Modifier.height(2.dp))
                RoleBadge(text = stringResource(label))
            }
        }
    }
}

@Composable
private fun RoleBadge(
    text: String,
    modifier: Modifier = Modifier,
    leadingSymbol: MaterialSymbol? = null,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            leadingSymbol?.let { symbol ->
                MaterialSymbolIcon(
                    symbol = symbol,
                    contentDescription = null,
                    size = 14.dp,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SportsCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    secondaryContainerColor: Color? = null,
    contentColor: Color? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val pitchPalette = DroolThemeDefaults.pitchCardPalette
    val resolvedPrimaryTone = containerColor ?: pitchPalette.primaryTone
    val resolvedSecondaryTone = secondaryContainerColor
        ?: if (containerColor == null) {
            pitchPalette.secondaryTone
        } else {
            containerColor.secondPitchTone()
        }
    val resolvedContentColor = contentColor ?: pitchPalette.contentColor
    val watermarkColor = if (containerColor == null) {
        pitchPalette.watermarkColor
    } else {
        Color.White.copy(
            alpha = if (resolvedSecondaryTone.luminance() > LUMINANCE_THRESHOLD) {
                LIGHT_SURFACE_WATERMARK_ALPHA
            } else {
                DARK_SURFACE_WATERMARK_ALPHA
            },
        )
    }
    val shape = RoundedCornerShape(
        topStart = CARD_ROUNDED_CORNER_DP.dp,
        topEnd = 0.dp,
        bottomEnd = CARD_ROUNDED_CORNER_DP.dp,
        bottomStart = CARD_ROUNDED_CORNER_DP.dp,
    )
    val colors = CardDefaults.cardColors(
        containerColor = resolvedPrimaryTone,
        contentColor = resolvedContentColor,
        disabledContainerColor = resolvedPrimaryTone.copy(alpha = 0.6f),
        disabledContentColor = resolvedContentColor.copy(alpha = 0.6f),
    )
    val decoratedContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .pitchCardDecoration(
                    secondaryTone = resolvedSecondaryTone,
                    watermarkColor = watermarkColor,
                ),
            content = content,
        )
    }
    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            content = decoratedContent,
        )
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            content = decoratedContent,
        )
    }
}

private fun Modifier.pitchCardDecoration(
    secondaryTone: Color,
    watermarkColor: Color,
): Modifier = drawWithCache {
    val cellStepPx = GRASS_CELL_SPACING_DP.dp.toPx()
    val bladeHeightPx = GRASS_BLADE_HEIGHT_DP.dp.toPx()
    val bladeSwayPx = GRASS_BLADE_SWAY_DP.dp.toPx()
    val grassPaths = buildGrassTexturePaths(
        width = size.width,
        height = size.height,
        cellStepPx = cellStepPx,
        bladeHeightPx = bladeHeightPx,
        bladeSwayPx = bladeSwayPx,
    )
    val bladeStroke = Stroke(
        width = GRASS_BLADE_STROKE_DP.dp.toPx(),
        cap = StrokeCap.Round,
    )
    val isLightSurface = secondaryTone.luminance() > LUMINANCE_THRESHOLD
    val highlightBladeColor = lerp(
        secondaryTone,
        Color.White,
        if (isLightSurface) GRASS_LIGHT_HIGHLIGHT_MIX else GRASS_DARK_HIGHLIGHT_MIX,
    ).copy(alpha = if (isLightSurface) GRASS_LIGHT_HIGHLIGHT_ALPHA else GRASS_DARK_HIGHLIGHT_ALPHA)
    val shadowBladeColor = lerp(
        secondaryTone,
        Color.Black,
        if (isLightSurface) GRASS_LIGHT_SHADOW_MIX else GRASS_DARK_SHADOW_MIX,
    ).copy(alpha = if (isLightSurface) GRASS_LIGHT_SHADOW_ALPHA else GRASS_DARK_SHADOW_ALPHA)
    val sheenBrush = Brush.linearGradient(
        colors = listOf(
            secondaryTone.copy(alpha = 0.42f),
            Color.Transparent,
        ),
        start = Offset(x = size.width, y = 0f),
        end = Offset(x = 0f, y = size.height),
    )
    onDrawBehind {
        drawRect(brush = sheenBrush)
        drawPath(
            path = grassPaths.shadowBladesPath,
            color = shadowBladeColor,
            style = bladeStroke,
        )
        drawPath(
            path = grassPaths.highlightBladesPath,
            color = highlightBladeColor,
            style = bladeStroke,
        )
        drawPitchCornerWatermark(watermarkColor)
    }
}

internal data class GrassTexturePaths(
    val highlightBladesPath: Path,
    val shadowBladesPath: Path,
    val tuftCount: Int,
)

internal fun buildGrassTexturePaths(
    width: Float,
    height: Float,
    cellStepPx: Float,
    bladeHeightPx: Float,
    bladeSwayPx: Float,
): GrassTexturePaths {
    val highlightPath = Path()
    val shadowPath = Path()
    if (width <= 0f || height <= 0f || cellStepPx <= 0f) {
        return GrassTexturePaths(highlightPath, shadowPath, tuftCount = 0)
    }
    val columns = (width / cellStepPx).toInt().coerceAtLeast(1)
    val rows = (height / cellStepPx).toInt().coerceAtLeast(1)
    var count = 0
    for (row in 0 until rows) {
        for (col in 0 until columns) {
            val hash = grassCellHash(col, row)
            val jitterX = ((hash and 0xFF) / 255f - 0.5f) * cellStepPx * 0.7f
            val jitterY = (((hash ushr 8) and 0xFF) / 255f - 0.5f) * cellStepPx * 0.7f
            val rootX = (col + 0.5f) * cellStepPx + jitterX
            val rootY = (row + 0.5f) * cellStepPx + jitterY
            val heightFactor = 0.65f + (((hash ushr 16) and 0x7F) / 127f) * 0.55f
            val swayFactor = 0.6f + (((hash ushr 23) and 0x3F) / 63f) * 0.6f
            val h1 = bladeHeightPx * heightFactor
            val h2 = bladeHeightPx * (heightFactor * 0.82f)
            val sway = bladeSwayPx * swayFactor
            val targetPath = if ((hash and 1) == 0) highlightPath else shadowPath
            targetPath.moveTo(rootX, rootY)
            targetPath.lineTo(rootX - sway, rootY - h1)
            targetPath.moveTo(rootX, rootY)
            targetPath.lineTo(rootX + sway * GRASS_SECONDARY_SWAY_SCALE, rootY - h2)
            count++
        }
    }
    return GrassTexturePaths(
        highlightBladesPath = highlightPath,
        shadowBladesPath = shadowPath,
        tuftCount = count,
    )
}

private fun grassCellHash(
    col: Int,
    row: Int,
): Int {
    var h = col * GRASS_HASH_PRIME_X xor row * GRASS_HASH_PRIME_Y
    h = h xor (h ushr GRASS_HASH_SHIFT_A)
    h *= GRASS_HASH_MIX
    return h xor (h ushr GRASS_HASH_SHIFT_B)
}

internal data class TurfStripeCoordinates(
    val bandStartTopX: Float,
    val bandEndTopX: Float,
    val bandStartBottomX: Float,
    val bandEndBottomX: Float,
    val splitStartTopX: Float,
    val splitStartBottomX: Float,
)

internal fun computeTurfStripeCoordinates(
    width: Float,
    height: Float,
): TurfStripeCoordinates {
    val horizontalShift = height * TURF_STRIPE_SLOPE
    val bandStartTopX = width * TURF_BAND_TOP_START_FRACTION
    val bandEndTopX = width * TURF_BAND_TOP_END_FRACTION
    val splitStartTopX = width * TURF_SPLIT_TOP_FRACTION
    return TurfStripeCoordinates(
        bandStartTopX = bandStartTopX,
        bandEndTopX = bandEndTopX,
        bandStartBottomX = bandStartTopX - horizontalShift,
        bandEndBottomX = bandEndTopX - horizontalShift,
        splitStartTopX = splitStartTopX,
        splitStartBottomX = splitStartTopX - horizontalShift,
    )
}

internal data class PitchCornerBorderGeometry(
    val cornerX: Float,
    val cornerY: Float,
    val topLineStartX: Float,
    val rightLineEndY: Float,
    val arcRadius: Float,
)

internal fun computePitchCornerBorderGeometry(
    width: Float,
    height: Float,
    strokeWidthPx: Float,
    arcRadiusPx: Float,
): PitchCornerBorderGeometry {
    val halfStroke = strokeWidthPx / 2f
    return PitchCornerBorderGeometry(
        cornerX = width - halfStroke,
        cornerY = halfStroke,
        topLineStartX = width * (1f - WATERMARK_EDGE_FRACTION),
        rightLineEndY = height * WATERMARK_EDGE_FRACTION,
        arcRadius = arcRadiusPx,
    )
}

private fun DrawScope.drawPitchCornerWatermark(
    color: Color,
) {
    if (size.width <= 0f || size.height <= 0f) return
    val strokeWidthPx = WATERMARK_STROKE_DP.dp.toPx()
    val geometry = computePitchCornerBorderGeometry(
        width = size.width,
        height = size.height,
        strokeWidthPx = strokeWidthPx,
        arcRadiusPx = WATERMARK_ARC_RADIUS_DP.dp.toPx(),
    )

    val stroke = Stroke(
        width = strokeWidthPx,
        cap = StrokeCap.Square,
        join = StrokeJoin.Miter,
    )

    // Top touchline and right goal line acting as the border along 100% of the top and right edges
    val cornerLinesPath = Path().apply {
        moveTo(geometry.topLineStartX, geometry.cornerY)
        lineTo(geometry.cornerX, geometry.cornerY)
        lineTo(geometry.cornerX, geometry.rightLineEndY)
    }
    drawPath(path = cornerLinesPath, color = color, style = stroke)

    // Quarter-circle corner arc with a consistent fixed radius
    if (geometry.arcRadius > 0f) {
        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(
                geometry.cornerX - geometry.arcRadius,
                geometry.cornerY - geometry.arcRadius,
            ),
            size = Size(geometry.arcRadius * 2f, geometry.arcRadius * 2f),
            style = stroke,
        )
    }
}

internal fun Color.secondPitchTone(): Color = if (luminance() > LUMINANCE_THRESHOLD) {
    lerp(this, Color.Black, LIGHT_TONE_SHIFT_FRACTION)
} else {
    lerp(this, Color.White, DARK_TONE_SHIFT_FRACTION)
}

private fun TeamRelationship.labelResource() = when (this) {
    TeamRelationship.None -> null
    TeamRelationship.Following -> Res.string.team_relationship_following
    TeamRelationship.Player -> Res.string.team_relationship_player
    TeamRelationship.Coaching -> Res.string.team_relationship_coach
    TeamRelationship.Managing -> Res.string.team_relationship_manager
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
private const val LUMINANCE_THRESHOLD = 0.30f
private const val LIGHT_TONE_SHIFT_FRACTION = 0.08f
private const val DARK_TONE_SHIFT_FRACTION = 0.09f
private const val LIGHT_SURFACE_WATERMARK_ALPHA = 0.85f
private const val DARK_SURFACE_WATERMARK_ALPHA = 0.34f
private const val CARD_ROUNDED_CORNER_DP = 18
private const val TURF_STRIPE_SLOPE = 0.36f
private const val TURF_BAND_TOP_START_FRACTION = 0.32f
private const val TURF_BAND_TOP_END_FRACTION = 0.48f
private const val TURF_SPLIT_TOP_FRACTION = 0.60f
private const val GRASS_CELL_SPACING_DP = 11f
private const val GRASS_BLADE_HEIGHT_DP = 5.5f
private const val GRASS_BLADE_SWAY_DP = 2.2f
private const val GRASS_BLADE_STROKE_DP = 1.1f
private const val GRASS_SECONDARY_SWAY_SCALE = 0.85f
private const val GRASS_LIGHT_HIGHLIGHT_MIX = 0.55f
private const val GRASS_DARK_HIGHLIGHT_MIX = 0.35f
private const val GRASS_LIGHT_HIGHLIGHT_ALPHA = 0.22f
private const val GRASS_DARK_HIGHLIGHT_ALPHA = 0.14f
private const val GRASS_LIGHT_SHADOW_MIX = 0.32f
private const val GRASS_DARK_SHADOW_MIX = 0.45f
private const val GRASS_LIGHT_SHADOW_ALPHA = 0.10f
private const val GRASS_DARK_SHADOW_ALPHA = 0.18f
private const val GRASS_HASH_PRIME_X = 73856093
private const val GRASS_HASH_PRIME_Y = 19349663
private const val GRASS_HASH_MIX = 0x5bd1e995
private const val GRASS_HASH_SHIFT_A = 13
private const val GRASS_HASH_SHIFT_B = 16
private const val WATERMARK_EDGE_FRACTION = 1f
private const val WATERMARK_ARC_RADIUS_DP = 44f
private const val WATERMARK_STROKE_DP = 2f
