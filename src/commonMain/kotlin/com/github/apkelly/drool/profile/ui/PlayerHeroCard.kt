@file:Suppress("MagicNumber")

package com.github.apkelly.drool.profile.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.TeamRelationship
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.content_profile_image
import com.github.apkelly.drool.common.resources.player_card_archetype_all_rounder
import com.github.apkelly.drool.common.resources.player_card_archetype_defender
import com.github.apkelly.drool.common.resources.player_card_archetype_finisher
import com.github.apkelly.drool.common.resources.player_card_archetype_playmaker
import com.github.apkelly.drool.common.resources.player_card_attack
import com.github.apkelly.drool.common.resources.player_card_defence
import com.github.apkelly.drool.common.resources.player_card_edition
import com.github.apkelly.drool.common.resources.player_card_fantasy
import com.github.apkelly.drool.common.resources.player_card_gender
import com.github.apkelly.drool.common.resources.player_card_midfield
import com.github.apkelly.drool.common.resources.player_card_role_coach
import com.github.apkelly.drool.common.resources.player_card_role_manager
import com.github.apkelly.drool.common.resources.player_card_subtitle
import com.github.apkelly.drool.common.resources.player_card_total
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.common.ui.widgets.RemoteImage
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlayerHeroCard(
    user: RelatedUser,
    modifier: Modifier = Modifier,
    role: PlayerCardRole = PlayerCardRole.Player,
) {
    val card = remember(user.subjectUserId) { fantasyPlayerCard(user) }
    val sensorTilt by rememberPlayerCardTilt()
    var pointerTilt by remember { mutableStateOf<PlayerCardTilt?>(null) }
    val targetTilt = pointerTilt ?: sensorTilt
    val horizontal by animateFloatAsState(
        targetValue = targetTilt.horizontal,
        animationSpec = spring(stiffness = 220f, dampingRatio = 0.72f),
    )
    val vertical by animateFloatAsState(
        targetValue = targetTilt.vertical,
        animationSpec = spring(stiffness = 220f, dampingRatio = 0.72f),
    )
    val shimmer by rememberInfiniteTransition().animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(FOIL_ANIMATION_MILLIS),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val outerShape = RoundedCornerShape(24.dp)
    val innerShape = RoundedCornerShape(19.dp)

    Box(
        modifier = modifier
            .widthIn(max = playerCardMaximumWidth)
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT_RATIO)
            .pointerInput(Unit) {
                fun updatePointerTilt(
                    position: Offset,
                ) {
                    pointerTilt = PlayerCardTilt(
                        horizontal = ((position.x / size.width) * 2f - 1f)
                            .coerceIn(-1f, 1f),
                        vertical = ((position.y / size.height) * 2f - 1f)
                            .coerceIn(-1f, 1f),
                    )
                }
                detectDragGestures(
                    onDragStart = ::updatePointerTilt,
                    onDragEnd = { pointerTilt = null },
                    onDragCancel = { pointerTilt = null },
                ) { change, _ ->
                    change.consume()
                    updatePointerTilt(change.position)
                }
            }
            .graphicsLayer {
                rotationX = -vertical * MAX_TILT_DEGREES
                rotationY = horizontal * MAX_TILT_DEGREES
                cameraDistance = density * CAMERA_DISTANCE
                transformOrigin = TransformOrigin.Center
            }
            .shadow(18.dp, outerShape)
            .clip(outerShape)
            .background(
                Brush.linearGradient(
                    listOf(FoilGold, FoilEmerald, FoilCyan, FoilPink, FoilGold),
                ),
            )
            .padding(5.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(innerShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            CardEmerald,
                            CardTeal,
                            CardNavy,
                            CardEmerald,
                            Color(0xFF14532D),
                            CardEmerald,
                        ),
                    ),
                )
                .drawBehind {
                    val gap = 18.dp.toPx()
                    var x = 0f
                    while (x <= size.width) {
                        var y = 0f
                        while (y <= size.height) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = 1.dp.toPx(),
                                center = Offset(x, y),
                            )
                            y += gap
                        }
                        x += gap
                    }
                },
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                PlayerCardHeader(card, role)
                PlayerPortrait(user, card, role, Modifier.weight(1f))
                if (role == PlayerCardRole.Player) {
                    PlayerStats(card, user.gender)
                }
                PlayerNamePlate(user, card, role)
            }
            HolographicFoil(
                horizontal = horizontal,
                vertical = vertical,
                shimmer = shimmer,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PlayerCardHeader(
    card: FantasyPlayerCard,
    role: PlayerCardRole,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(58.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DROOL",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(horizontal = 9.dp, vertical = 2.dp),
            )
            Text(
                text = card.cardNumber,
                color = Color.Black,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 1.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(
                    Brush.verticalGradient(listOf(Color.White, Color(0xFFFDE68A))),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("★★★", color = Color(0xFFD97706), fontSize = 8.sp)
                Text(
                    text = when (role) {
                        PlayerCardRole.Player -> card.jerseyNumber.toString()
                        PlayerCardRole.Coach -> "C"
                        PlayerCardRole.Manager -> "M"
                    },
                    color = CardEmerald,
                    fontSize = 22.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Text(
            text = stringResource(Res.string.player_card_edition).uppercase(),
            color = Color(0xFF111827),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.horizontalGradient(listOf(FoilGold, Color(0xFFFDE68A))))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PlayerPortrait(
    user: RelatedUser,
    card: FantasyPlayerCard,
    role: PlayerCardRole,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        RemoteImage(
            url = user.avatarUrl,
            contentDescription = stringResource(
                Res.string.content_profile_image,
                user.displayName,
            ),
            fallbackIcon = MaterialSymbol.Person,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.84f)
                .align(Alignment.Center),
            shape = RoundedCornerShape(20.dp),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = stringResource(
                role.labelResource ?: card.archetype.resource,
            ).uppercase(),
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .graphicsLayer { rotationZ = 90f },
        )
        if (role == PlayerCardRole.Player) {
            Text(
                text = stringResource(Res.string.player_card_fantasy).uppercase(),
                color = Color(0xFFFDE68A),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun PlayerStats(
    card: FantasyPlayerCard,
    gender: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StatPill(
            label = stringResource(Res.string.player_card_attack),
            value = card.attack.toString(),
            colors = listOf(Color(0xFFEF4444), Color(0xFF991B1B)),
            modifier = Modifier.weight(1f),
        )
        StatPill(
            label = stringResource(Res.string.player_card_midfield),
            value = card.midfield.toString(),
            colors = listOf(Color(0xFF0EA5E9), Color(0xFF0E7490)),
            modifier = Modifier.weight(1f),
        )
        StatPill(
            label = stringResource(Res.string.player_card_defence),
            value = card.defence.toString(),
            colors = listOf(Color(0xFF22C55E), Color(0xFF15803D)),
            modifier = Modifier.weight(1f),
        )
        playerCardGender(gender)?.let { value ->
            StatPill(
                label = stringResource(Res.string.player_card_gender),
                value = value,
                colors = listOf(Color(0xFFC084FC), Color(0xFF7E22CE)),
                modifier = Modifier.weight(1f),
            )
        }
        StatPill(
            label = stringResource(Res.string.player_card_total),
            value = card.total.toString(),
            colors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B)),
            valueColor = Color(0xFF111827),
            modifier = Modifier.weight(1.12f),
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(colors))
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = valueColor.copy(alpha = 0.86f),
            fontSize = 8.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 18.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun PlayerNamePlate(
    user: RelatedUser,
    card: FantasyPlayerCard,
    role: PlayerCardRole,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName.uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = role.labelResource?.let { stringResource(it).uppercase() }
                    ?: stringResource(
                        Res.string.player_card_subtitle,
                        card.jerseyNumber,
                        stringResource(card.archetype.resource).uppercase(),
                    ),
                color = FoilGold,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(
                    Brush.linearGradient(listOf(FoilGold, FoilEmerald, FoilCyan)),
                )
                .padding(2.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "D",
                color = FoilGold,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HolographicFoil(
    horizontal: Float,
    vertical: Float,
    shimmer: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val centerX = size.width * (0.5f + horizontal * 0.42f + shimmer * 0.18f)
        val centerY = size.height * (0.5f + vertical * 0.32f)
        val extent = size.minDimension * 0.75f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    FoilPink.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.42f),
                    FoilGold.copy(alpha = 0.28f),
                    FoilCyan.copy(alpha = 0.25f),
                    Color.Transparent,
                ),
                start = Offset(centerX - extent, centerY - extent),
                end = Offset(centerX + extent, centerY + extent),
            ),
            blendMode = BlendMode.Screen,
        )
    }
}

internal data class FantasyPlayerCard(
    val jerseyNumber: Int,
    val cardNumber: String,
    val attack: Int,
    val midfield: Int,
    val defence: Int,
    val archetype: PlayerArchetype,
) {
    val total = attack + midfield + defence
}

internal enum class PlayerArchetype(
    val resource: org.jetbrains.compose.resources.StringResource,
) {
    Playmaker(Res.string.player_card_archetype_playmaker),
    Finisher(Res.string.player_card_archetype_finisher),
    Defender(Res.string.player_card_archetype_defender),
    AllRounder(Res.string.player_card_archetype_all_rounder),
}

enum class PlayerCardRole(
    val labelResource: org.jetbrains.compose.resources.StringResource?,
) {
    Player(null),
    Coach(Res.string.player_card_role_coach),
    Manager(Res.string.player_card_role_manager),
}

internal fun playerCardRole(
    relationships: Iterable<TeamRelationship>,
): PlayerCardRole {
    val values = relationships.toSet()
    return when {
        TeamRelationship.Managing in values -> PlayerCardRole.Manager
        TeamRelationship.Coaching in values -> PlayerCardRole.Coach
        else -> PlayerCardRole.Player
    }
}

internal fun fantasyPlayerCard(
    user: RelatedUser,
): FantasyPlayerCard {
    val seed = user.subjectUserId.fold(17) { value, character ->
        (value * 31 + character.code) and Int.MAX_VALUE
    }
    val attack = FANTASY_STAT_MINIMUM + seed % FANTASY_STAT_SPREAD
    val midfield = FANTASY_STAT_MINIMUM + (seed / 7) % FANTASY_STAT_SPREAD
    val defence = FANTASY_STAT_MINIMUM + (seed / 13) % FANTASY_STAT_SPREAD
    val archetype = when {
        attack > midfield + ARCHETYPE_MARGIN && attack > defence + ARCHETYPE_MARGIN ->
            PlayerArchetype.Finisher

        midfield > attack + ARCHETYPE_MARGIN && midfield > defence + ARCHETYPE_MARGIN ->
            PlayerArchetype.Playmaker

        defence > attack + ARCHETYPE_MARGIN && defence > midfield + ARCHETYPE_MARGIN ->
            PlayerArchetype.Defender

        else -> PlayerArchetype.AllRounder
    }
    return FantasyPlayerCard(
        jerseyNumber = JERSEY_MINIMUM + seed % JERSEY_SPREAD,
        cardNumber = "#${((seed % CARD_NUMBER_SPREAD) + 1).toString().padStart(3, '0')}",
        attack = attack,
        midfield = midfield,
        defence = defence,
        archetype = archetype,
    )
}

internal fun playerCardGender(
    gender: String?,
): String? {
    val value = gender?.trim().orEmpty()
    return when (value.lowercase()) {
        "", "-", "unknown", "unspecified", "not specified", "n/a" -> null
        "m", "male", "man", "boy" -> "M"
        "f", "female", "woman", "girl" -> "F"
        "nb", "nonbinary", "non-binary", "non binary" -> "NB"
        else -> value.uppercase().take(GENDER_DISPLAY_LIMIT)
    }
}

private val FoilGold = Color(0xFFFBBF24)
private val FoilEmerald = Color(0xFF34D399)
private val FoilCyan = Color(0xFF22D3EE)
private val FoilPink = Color(0xFFF472B6)
private val CardEmerald = Color(0xFF022C22)
private val CardTeal = Color(0xFF0F766E)
private val CardNavy = Color(0xFF0F172A)
private const val CARD_ASPECT_RATIO = 0.704f
private const val CAMERA_DISTANCE = 18f
private const val MAX_TILT_DEGREES = 9f
private const val FOIL_ANIMATION_MILLIS = 4_500
private const val FANTASY_STAT_MINIMUM = 64
private const val FANTASY_STAT_SPREAD = 34
private const val ARCHETYPE_MARGIN = 5
private const val JERSEY_MINIMUM = 1
private const val JERSEY_SPREAD = 30
private const val CARD_NUMBER_SPREAD = 999
private const val GENDER_DISPLAY_LIMIT = 3
