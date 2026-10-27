package com.github.apkelly.drool.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.AssignmentPollingInterval
import com.github.apkelly.drool.common.domain.model.LinkMemberFailure
import com.github.apkelly.drool.common.domain.model.Profile
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.domain.model.ThemeMode
import com.github.apkelly.drool.common.observability.platformSupportsObservability
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_cancel
import com.github.apkelly.drool.common.resources.action_sign_out
import com.github.apkelly.drool.common.resources.assignment_polling_hourly
import com.github.apkelly.drool.common.resources.assignment_polling_off
import com.github.apkelly.drool.common.resources.assignment_polling_six_hours
import com.github.apkelly.drool.common.resources.assignment_polling_three_hours
import com.github.apkelly.drool.common.resources.content_profile_image
import com.github.apkelly.drool.common.resources.profile_appearance
import com.github.apkelly.drool.common.resources.profile_assignment_checks
import com.github.apkelly.drool.common.resources.profile_assignment_checks_body
import com.github.apkelly.drool.common.resources.profile_link_member
import com.github.apkelly.drool.common.resources.profile_no_related_users
import com.github.apkelly.drool.common.resources.profile_observability
import com.github.apkelly.drool.common.resources.profile_observability_body
import com.github.apkelly.drool.common.resources.profile_related_users
import com.github.apkelly.drool.common.resources.profile_title
import com.github.apkelly.drool.common.resources.profile_unlink_action
import com.github.apkelly.drool.common.resources.profile_unlink_body
import com.github.apkelly.drool.common.resources.profile_unlink_failed
import com.github.apkelly.drool.common.resources.profile_unlink_member
import com.github.apkelly.drool.common.resources.profile_unlink_offline
import com.github.apkelly.drool.common.resources.profile_unlink_session_expired
import com.github.apkelly.drool.common.resources.profile_unlink_title
import com.github.apkelly.drool.common.resources.sign_out_body
import com.github.apkelly.drool.common.resources.sign_out_offline_warning
import com.github.apkelly.drool.common.resources.sign_out_title
import com.github.apkelly.drool.common.resources.theme_dark
import com.github.apkelly.drool.common.resources.theme_light
import com.github.apkelly.drool.common.resources.theme_system
import com.github.apkelly.drool.common.ui.preview.TopLevelScreenPreviewData
import com.github.apkelly.drool.common.ui.theme.DroolTheme
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbolIcon
import com.github.apkelly.drool.common.ui.widgets.RemoteImage
import com.github.apkelly.drool.common.ui.widgets.SportsCard
import com.github.apkelly.drool.profile.ui.model.ProfileUiModel
import com.github.apkelly.drool.profile.ui.model.UnlinkMemberUiState
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    model: ProfileUiModel,
    onThemeChange: (ThemeMode) -> Unit,
    onObservabilityChange: (Boolean) -> Unit,
    onAssignmentPollingIntervalChange: (AssignmentPollingInterval) -> Unit,
    onAddMember: () -> Unit,
    onRelatedUserSelect: (RelatedUser) -> Unit,
    onUnlinkUser: (String) -> Unit,
    onClearUnlinkFailure: () -> Unit,
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit,
) {
    val profile = model.profile
    var confirmSignOut by remember { mutableStateOf(false) }
    var pendingUnlink by remember { mutableStateOf<RelatedUser?>(null) }
    val linkedUsers = profile.relatedUsers
        .filter { it.isLinked && it.subjectUserId != profile.accountId }
        .distinctBy { it.subjectUserId }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState(),
    )
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.profile_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMember,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                MaterialSymbolIcon(
                    MaterialSymbol.Add,
                    contentDescription = stringResource(Res.string.profile_link_member),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProfileHeaderCard(profile = profile)
            }
            item {
                Text(
                    stringResource(Res.string.profile_related_users),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (linkedUsers.isEmpty()) {
                item {
                    SportsCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(Res.string.profile_no_related_users),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(linkedUsers, key = { it.subjectUserId }) { user ->
                    val linkId = user.linkId
                    ProfileAssociationCard(
                        name = user.displayName,
                        subtitle = user.email,
                        imageUrl = user.avatarUrl,
                        contentDescription = stringResource(
                            Res.string.content_profile_image,
                            user.displayName,
                        ),
                        fallbackIcon = MaterialSymbol.Person,
                        onClick = { onRelatedUserSelect(user) },
                        action = linkId?.let {
                            {
                                UnlinkMemberAction(
                                    name = user.displayName,
                                    linkId = it,
                                    state = model.unlinkMemberState,
                                    onClick = { pendingUnlink = user },
                                )
                            }
                        },
                    )
                }
            }
            item {
                AppearanceSettingsCard(
                    themeMode = model.themeMode,
                    onThemeChange = onThemeChange,
                )
            }
            if (platformSupportsObservability) {
                item {
                    ObservabilitySettingsCard(
                        observabilityEnabled = model.observabilityEnabled,
                        onObservabilityChange = onObservabilityChange,
                    )
                }
            }
            item {
                AssignmentPollingSettingsCard(
                    assignmentPollingInterval = model.assignmentPollingInterval,
                    onAssignmentPollingIntervalChange = onAssignmentPollingIntervalChange,
                )
            }
            item {
                OutlinedButton(onClick = { confirmSignOut = true }) {
                    Text(stringResource(Res.string.action_sign_out))
                }
            }
        }
    }

    ProfileDialogs(
        confirmSignOut = confirmSignOut,
        onDismissSignOut = { confirmSignOut = false },
        onConfirmSignOut = {
            confirmSignOut = false
            onSignOut()
        },
        pendingUnlink = pendingUnlink,
        onDismissUnlink = { pendingUnlink = null },
        onConfirmUnlink = { user ->
            pendingUnlink = null
            user.linkId?.let(onUnlinkUser)
        },
        unlinkMemberState = model.unlinkMemberState,
        onClearUnlinkFailure = onClearUnlinkFailure,
    )
}

@Composable
private fun ProfileHeaderCard(
    profile: Profile,
) {
    SportsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteImage(
                url = profile.avatarUrl,
                contentDescription = stringResource(
                    Res.string.content_profile_image,
                    profile.displayName,
                ),
                fallbackIcon = MaterialSymbol.Person,
                modifier = Modifier.size(64.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    profile.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                profile.email?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceSettingsCard(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    SportsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.profile_appearance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeChange(mode) },
                        label = {
                            Text(
                                stringResource(
                                    when (mode) {
                                        ThemeMode.System -> Res.string.theme_system
                                        ThemeMode.Light -> Res.string.theme_light
                                        ThemeMode.Dark -> Res.string.theme_dark
                                    },
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ObservabilitySettingsCard(
    observabilityEnabled: Boolean,
    onObservabilityChange: (Boolean) -> Unit,
) {
    SportsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(Res.string.profile_observability),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(Res.string.profile_observability_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = observabilityEnabled,
                onCheckedChange = onObservabilityChange,
            )
        }
    }
}

@Composable
private fun AssignmentPollingSettingsCard(
    assignmentPollingInterval: AssignmentPollingInterval,
    onAssignmentPollingIntervalChange: (AssignmentPollingInterval) -> Unit,
) {
    SportsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(Res.string.profile_assignment_checks),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(Res.string.profile_assignment_checks_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AssignmentPollingInterval.entries) { interval ->
                    FilterChip(
                        selected = assignmentPollingInterval == interval,
                        onClick = { onAssignmentPollingIntervalChange(interval) },
                        label = { Text(stringResource(interval.labelResource())) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileDialogs(
    confirmSignOut: Boolean,
    onDismissSignOut: () -> Unit,
    onConfirmSignOut: () -> Unit,
    pendingUnlink: RelatedUser?,
    onDismissUnlink: () -> Unit,
    onConfirmUnlink: (RelatedUser) -> Unit,
    unlinkMemberState: UnlinkMemberUiState,
    onClearUnlinkFailure: () -> Unit,
) {
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = onDismissSignOut,
            title = { Text(stringResource(Res.string.sign_out_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.sign_out_body))
                    Text(stringResource(Res.string.sign_out_offline_warning))
                }
            },
            confirmButton = {
                Button(onClick = onConfirmSignOut) {
                    Text(stringResource(Res.string.action_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSignOut) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    pendingUnlink?.let { user ->
        AlertDialog(
            onDismissRequest = onDismissUnlink,
            title = {
                Text(stringResource(Res.string.profile_unlink_title, user.displayName))
            },
            text = {
                Text(stringResource(Res.string.profile_unlink_body, user.displayName))
            },
            confirmButton = {
                Button(onClick = { onConfirmUnlink(user) }) {
                    Text(stringResource(Res.string.profile_unlink_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUnlink) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    (unlinkMemberState as? UnlinkMemberUiState.Failed)?.let { failure ->
        AlertDialog(
            onDismissRequest = onClearUnlinkFailure,
            title = { Text(stringResource(Res.string.profile_unlink_failed)) },
            text = {
                Text(
                    stringResource(
                        when (failure.reason) {
                            LinkMemberFailure.SessionExpired ->
                                Res.string.profile_unlink_session_expired

                            LinkMemberFailure.Offline -> Res.string.profile_unlink_offline

                            LinkMemberFailure.InvalidToken,
                            LinkMemberFailure.Unknown,
                            -> Res.string.profile_unlink_failed
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onClearUnlinkFailure) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ProfileAssociationCard(
    name: String,
    subtitle: String?,
    imageUrl: String?,
    contentDescription: String,
    fallbackIcon: MaterialSymbol,
    onClick: (() -> Unit)?,
    action: (@Composable () -> Unit)? = null,
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteImage(
                url = imageUrl,
                contentDescription = contentDescription,
                fallbackIcon = fallbackIcon,
                modifier = Modifier.size(44.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            action?.invoke()
        }
    }
    SportsCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}

@Composable
private fun UnlinkMemberAction(
    name: String,
    linkId: String,
    state: UnlinkMemberUiState,
    onClick: () -> Unit,
) {
    val unlinking = state as? UnlinkMemberUiState.Unlinking
    if (unlinking?.linkId == linkId) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
        )
    } else {
        IconButton(
            onClick = onClick,
            enabled = unlinking == null,
        ) {
            MaterialSymbolIcon(
                symbol = MaterialSymbol.LinkOff,
                contentDescription = stringResource(Res.string.profile_unlink_member, name),
            )
        }
    }
}

@Preview(
    name = "Profile",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
private fun ProfileScreenPreview() {
    DroolTheme(mode = ThemeMode.Light) {
        ProfileScreen(
            model = TopLevelScreenPreviewData.profileModel,
            onThemeChange = {},
            onObservabilityChange = {},
            onAssignmentPollingIntervalChange = {},
            onAddMember = {},
            onRelatedUserSelect = {},
            onUnlinkUser = {},
            onClearUnlinkFailure = {},
            onSignOut = {},
        )
    }
}

private fun AssignmentPollingInterval.labelResource() = when (this) {
    AssignmentPollingInterval.Off -> Res.string.assignment_polling_off

    AssignmentPollingInterval.Hourly -> Res.string.assignment_polling_hourly

    AssignmentPollingInterval.EveryThreeHours ->
        Res.string.assignment_polling_three_hours

    AssignmentPollingInterval.EverySixHours ->
        Res.string.assignment_polling_six_hours
}
