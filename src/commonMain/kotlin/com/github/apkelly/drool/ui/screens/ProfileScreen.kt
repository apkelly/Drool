package com.github.apkelly.drool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.domain.model.Profile
import com.github.apkelly.drool.domain.model.ThemeMode
import com.github.apkelly.drool.domain.model.RelatedUser
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.action_cancel
import com.github.apkelly.drool.resources.action_sign_out
import com.github.apkelly.drool.resources.profile_appearance
import com.github.apkelly.drool.resources.profile_accounts
import com.github.apkelly.drool.resources.profile_no_accounts
import com.github.apkelly.drool.resources.profile_no_related_users
import com.github.apkelly.drool.resources.profile_related_users
import com.github.apkelly.drool.resources.profile_title
import com.github.apkelly.drool.resources.profile_link_member
import com.github.apkelly.drool.resources.profile_api_diagnostics
import com.github.apkelly.drool.resources.profile_observability
import com.github.apkelly.drool.resources.profile_observability_body
import com.github.apkelly.drool.resources.content_profile_image
import com.github.apkelly.drool.resources.content_account_logo
import com.github.apkelly.drool.resources.sign_out_body
import com.github.apkelly.drool.resources.sign_out_offline_warning
import com.github.apkelly.drool.resources.sign_out_title
import com.github.apkelly.drool.resources.theme_dark
import com.github.apkelly.drool.resources.theme_light
import com.github.apkelly.drool.resources.theme_system
import com.github.apkelly.drool.ui.widgets.RemoteImage
import com.github.apkelly.drool.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.ui.widgets.MaterialSymbolIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileScreen(
    profile: Profile,
    themeMode: ThemeMode,
    observabilityEnabled: Boolean,
    onThemeChanged: (ThemeMode) -> Unit,
    onObservabilityChanged: (Boolean) -> Unit,
    onOpenApiDiagnostics: () -> Unit,
    onAddMember: () -> Unit,
    onRelatedUserSelected: (RelatedUser) -> Unit,
    onSignOut: () -> Unit,
) {
    var confirmSignOut by remember { mutableStateOf(false) }
    val familyMembers = profile.relatedUsers
        .filter { it.isLinked && it.subjectUserId != profile.accountId }
        .distinctBy { it.subjectUserId }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMember) {
                MaterialSymbolIcon(
                    MaterialSymbol.Add,
                    contentDescription = stringResource(Res.string.profile_link_member),
                )
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
        item {
            Text(
                stringResource(Res.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RemoteImage(
                    url = profile.avatarUrl,
                    contentDescription = stringResource(
                        Res.string.content_profile_image,
                        profile.displayName,
                    ),
                    fallbackIcon = MaterialSymbol.Person,
                    modifier = Modifier.size(72.dp),
                    contentScale = ContentScale.Crop,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
                    profile.email?.let { Text(it) }
                }
            }
        }
        item {
            Text(
                stringResource(Res.string.profile_related_users),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (familyMembers.isEmpty()) {
            item { Text(stringResource(Res.string.profile_no_related_users)) }
        } else {
            items(familyMembers, key = { it.subjectUserId }) { user ->
                ProfileAssociationCard(
                    name = user.displayName,
                    subtitle = user.email,
                    imageUrl = user.avatarUrl,
                    contentDescription = stringResource(
                        Res.string.content_profile_image,
                        user.displayName,
                    ),
                    fallbackIcon = MaterialSymbol.Person,
                    onClick = { onRelatedUserSelected(user) },
                )
            }
        }
        item {
            Text(
                stringResource(Res.string.profile_accounts),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (profile.accounts.isEmpty()) {
            item { Text(stringResource(Res.string.profile_no_accounts)) }
        } else {
            items(profile.accounts, key = { it.id }) { account ->
                ProfileAssociationCard(
                    name = account.name,
                    subtitle = account.subtitle,
                    imageUrl = account.logoUrl,
                    contentDescription = stringResource(
                        Res.string.content_account_logo,
                        account.name,
                    ),
                    fallbackIcon = MaterialSymbol.AccountBalance,
                    onClick = null,
                )
            }
        }
        item {
            Text(
                stringResource(Res.string.profile_appearance),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeChanged(mode) },
                        label = {
                            Text(
                                stringResource(
                                    when (mode) {
                                        ThemeMode.System -> Res.string.theme_system
                                        ThemeMode.Light -> Res.string.theme_light
                                        ThemeMode.Dark -> Res.string.theme_dark
                                    }
                                )
                            )
                        },
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        stringResource(Res.string.profile_observability),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(Res.string.profile_observability_body),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = observabilityEnabled,
                    onCheckedChange = onObservabilityChanged,
                )
            }
        }
        item {
            OutlinedButton(onClick = onOpenApiDiagnostics) {
                Text(stringResource(Res.string.profile_api_diagnostics))
            }
        }
        item {
            OutlinedButton(onClick = { confirmSignOut = true }) {
                Text(stringResource(Res.string.action_sign_out))
            }
        }
    }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(Res.string.sign_out_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.sign_out_body))
                    Text(stringResource(Res.string.sign_out_offline_warning))
                }
            },
            confirmButton = {
                Button(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) {
                    Text(stringResource(Res.string.action_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) {
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
) {
    Card(
        modifier = Modifier.fillMaxWidth().let { modifier ->
            if (onClick == null) modifier else modifier.clickable(onClick = onClick)
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RemoteImage(
                url = imageUrl,
                contentDescription = contentDescription,
                fallbackIcon = fallbackIcon,
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Crop,
            )
            Column {
                Text(name, style = MaterialTheme.typography.titleSmall)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
