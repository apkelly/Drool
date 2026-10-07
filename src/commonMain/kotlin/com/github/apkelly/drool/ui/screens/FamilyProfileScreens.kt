package com.github.apkelly.drool.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.domain.model.EmergencyContact
import com.github.apkelly.drool.domain.model.LinkMemberFailure
import com.github.apkelly.drool.domain.model.RelatedUser
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.action_cancel
import com.github.apkelly.drool.resources.action_retry
import com.github.apkelly.drool.resources.content_profile_image
import com.github.apkelly.drool.resources.personal_address
import com.github.apkelly.drool.resources.personal_date_of_birth
import com.github.apkelly.drool.resources.personal_email
import com.github.apkelly.drool.resources.personal_emergency_contacts
import com.github.apkelly.drool.resources.personal_contact_call
import com.github.apkelly.drool.resources.personal_contact_email
import com.github.apkelly.drool.resources.personal_full_name
import com.github.apkelly.drool.resources.personal_gender
import com.github.apkelly.drool.resources.personal_not_provided
import com.github.apkelly.drool.resources.personal_phone
import com.github.apkelly.drool.resources.personal_title
import com.github.apkelly.drool.resources.profile_link_candidates_empty
import com.github.apkelly.drool.resources.profile_link_code
import com.github.apkelly.drool.resources.profile_link_code_body
import com.github.apkelly.drool.resources.profile_link_failed
import com.github.apkelly.drool.resources.profile_link_member
import com.github.apkelly.drool.resources.profile_link_session_expired
import com.github.apkelly.drool.resources.profile_link_verify
import com.github.apkelly.drool.ui.model.LinkMemberUiState
import com.github.apkelly.drool.ui.platform.rememberContactActionLauncher
import com.github.apkelly.drool.ui.widgets.RemoteImage
import com.github.apkelly.drool.ui.widgets.MaterialBackIcon
import com.github.apkelly.drool.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.ui.widgets.MaterialSymbolIcon
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInformationScreen(
    user: RelatedUser,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.personal_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MaterialBackIcon(contentDescription = null)
                    }
                },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    RemoteImage(
                        url = user.avatarUrl,
                        contentDescription = stringResource(
                            Res.string.content_profile_image,
                            user.displayName,
                        ),
                        fallbackIcon = MaterialSymbol.Person,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop,
                    )
                    Text(user.displayName, style = MaterialTheme.typography.headlineSmall)
                }
            }
            item { PersonalField(Res.string.personal_full_name, user.displayName) }
            item { PersonalField(Res.string.personal_gender, user.gender) }
            item { PersonalField(Res.string.personal_date_of_birth, user.dateOfBirth) }
            item { PersonalField(Res.string.personal_phone, user.phoneNumber) }
            item { PersonalField(Res.string.personal_email, user.email) }
            item { PersonalField(Res.string.personal_address, user.address) }
            item {
                EmergencyContactsField(user.emergencyContacts)
            }
        }
    }
}

@Composable
private fun EmergencyContactsField(contacts: List<EmergencyContact>) {
    val actionLauncher = rememberContactActionLauncher()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.personal_emergency_contacts),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (contacts.isEmpty()) {
            Text(stringResource(Res.string.personal_not_provided))
        } else {
            contacts.forEach { contact ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(contact.name, modifier = Modifier.weight(1f))
                    contact.phoneNumber?.takeIf(String::isNotBlank)?.let { phoneNumber ->
                        IconButton(
                            onClick = { actionLauncher.call(phoneNumber) }
                        ) {
                            MaterialSymbolIcon(
                                MaterialSymbol.Phone,
                                contentDescription = stringResource(
                                    Res.string.personal_contact_call,
                                    contact.name,
                                ),
                            )
                        }
                    }
                    contact.email?.takeIf(String::isNotBlank)?.let { email ->
                        IconButton(onClick = { actionLauncher.email(email) }) {
                            MaterialSymbolIcon(
                                MaterialSymbol.Email,
                                contentDescription = stringResource(
                                    Res.string.personal_contact_email,
                                    contact.name,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalField(label: StringResource, value: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(value ?: stringResource(Res.string.personal_not_provided))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkMemberScreen(
    state: LinkMemberUiState,
    onBack: () -> Unit,
    onLoad: () -> Unit,
    onVerify: (String, String) -> Unit,
    onLinked: () -> Unit,
) {
    var selected by remember { mutableStateOf<RelatedUser?>(null) }
    var token by remember(selected) { mutableStateOf("") }
    LaunchedEffect(Unit) {
        if (state == LinkMemberUiState.Idle) onLoad()
    }
    LaunchedEffect(state) {
        if (state == LinkMemberUiState.Linked) onLinked()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.profile_link_member)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MaterialBackIcon(contentDescription = null)
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state) {
                LinkMemberUiState.Idle,
                LinkMemberUiState.Loading -> CircularProgressIndicator()

                is LinkMemberUiState.Failed -> {
                    Text(
                        stringResource(
                            if (state.reason == LinkMemberFailure.SessionExpired) {
                                Res.string.profile_link_session_expired
                            } else {
                                Res.string.profile_link_failed
                            }
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onLoad) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }

                LinkMemberUiState.Linked -> CircularProgressIndicator()

                is LinkMemberUiState.Ready -> {
                    if (state.candidates.isEmpty()) {
                        Text(stringResource(Res.string.profile_link_candidates_empty))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.candidates, key = { it.id }) { candidate ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { selected = candidate },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        RemoteImage(
                                            url = candidate.avatarUrl,
                                            contentDescription = candidate.displayName,
                                            fallbackIcon = MaterialSymbol.Person,
                                            modifier = Modifier.size(44.dp),
                                        )
                                        Column {
                                            Text(
                                                candidate.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            candidate.email?.let { Text(it) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { candidate ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(candidate.displayName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(Res.string.profile_link_code_body))
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it.filter(Char::isDigit).take(6) },
                        label = { Text(stringResource(Res.string.profile_link_code)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onVerify(candidate.id, token) },
                    enabled = token.length == 6 &&
                        (state as? LinkMemberUiState.Ready)?.verifyingCandidateId == null,
                ) {
                    Text(stringResource(Res.string.profile_link_verify))
                }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}
