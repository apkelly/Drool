package com.github.apkelly.drool.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.EmergencyContact
import com.github.apkelly.drool.common.domain.model.LinkMemberFailure
import com.github.apkelly.drool.common.domain.model.RelatedUser
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_cancel
import com.github.apkelly.drool.common.resources.action_retry
import com.github.apkelly.drool.common.resources.personal_address
import com.github.apkelly.drool.common.resources.personal_contact_call
import com.github.apkelly.drool.common.resources.personal_contact_email
import com.github.apkelly.drool.common.resources.personal_date_of_birth
import com.github.apkelly.drool.common.resources.personal_email
import com.github.apkelly.drool.common.resources.personal_emergency_contacts
import com.github.apkelly.drool.common.resources.personal_full_name
import com.github.apkelly.drool.common.resources.personal_gender
import com.github.apkelly.drool.common.resources.personal_not_provided
import com.github.apkelly.drool.common.resources.personal_phone
import com.github.apkelly.drool.common.resources.personal_title
import com.github.apkelly.drool.common.resources.profile_link_candidates_empty
import com.github.apkelly.drool.common.resources.profile_link_code
import com.github.apkelly.drool.common.resources.profile_link_code_body
import com.github.apkelly.drool.common.resources.profile_link_failed
import com.github.apkelly.drool.common.resources.profile_link_guardian_body
import com.github.apkelly.drool.common.resources.profile_link_guardian_no
import com.github.apkelly.drool.common.resources.profile_link_guardian_title
import com.github.apkelly.drool.common.resources.profile_link_guardian_yes
import com.github.apkelly.drool.common.resources.profile_link_member
import com.github.apkelly.drool.common.resources.profile_link_session_expired
import com.github.apkelly.drool.common.resources.profile_link_verify
import com.github.apkelly.drool.common.resources.profile_tab_availability
import com.github.apkelly.drool.common.resources.profile_tab_card
import com.github.apkelly.drool.common.resources.profile_tab_personal
import com.github.apkelly.drool.common.ui.platform.rememberContactActionLauncher
import com.github.apkelly.drool.common.ui.widgets.MaterialBackIcon
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.common.ui.widgets.MaterialSymbolIcon
import com.github.apkelly.drool.common.ui.widgets.RemoteImage
import com.github.apkelly.drool.common.ui.widgets.SportsCard
import com.github.apkelly.drool.profile.ui.model.LinkMemberUiState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInformationScreen(
    user: RelatedUser,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    availabilityContent: (@Composable () -> Unit)? = null,
    cardRole: PlayerCardRole = PlayerCardRole.Player,
) {
    var selectedTabIndex by rememberSaveable(user.subjectUserId) {
        mutableIntStateOf(0)
    }
    val tabLabels = if (availabilityContent == null) {
        listOf(
            Res.string.profile_tab_card,
            Res.string.profile_tab_personal,
        )
    } else {
        listOf(
            Res.string.profile_tab_card,
            Res.string.profile_tab_personal,
            Res.string.profile_tab_availability,
        )
    }
    val selectedTab = selectedTabIndex.coerceIn(0, tabLabels.lastIndex)

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(Res.string.personal_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            MaterialBackIcon(contentDescription = null)
                        }
                    },
                )
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    tabLabels.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(stringResource(label)) },
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        when (selectedTab) {
            PROFILE_CARD_TAB -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(
                            start = 20.dp,
                            top = 12.dp,
                            end = 20.dp,
                            bottom = 20.dp,
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    PlayerHeroCard(user, role = cardRole)
                }
            }

            PERSONAL_INFORMATION_TAB -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
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

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                ) {
                    availabilityContent?.invoke()
                }
            }
        }
    }
}

private const val PROFILE_CARD_TAB = 0
private const val PERSONAL_INFORMATION_TAB = 1

@Composable
private fun EmergencyContactsField(
    contacts: List<EmergencyContact>,
) {
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
                            onClick = { actionLauncher.call(phoneNumber) },
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
private fun PersonalField(
    label: StringResource,
    value: String?,
) {
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
    onRequest: (RelatedUser, Boolean) -> Unit,
    onVerify: (String) -> Unit,
    onCancelVerification: () -> Unit,
    modifier: Modifier = Modifier,
    onLink: () -> Unit,
) {
    val currentOnLoad by rememberUpdatedState(onLoad)
    val currentOnLink by rememberUpdatedState(onLink)
    var selected by remember { mutableStateOf<RelatedUser?>(null) }
    var token by remember(state) { mutableStateOf("") }
    LaunchedEffect(Unit) {
        if (state == LinkMemberUiState.Idle) currentOnLoad()
    }
    LaunchedEffect(state) {
        if (state == LinkMemberUiState.Linked) currentOnLink()
    }

    Scaffold(
        modifier = modifier,
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
                LinkMemberUiState.Loading,
                -> CircularProgressIndicator()

                is LinkMemberUiState.Failed -> {
                    Text(
                        stringResource(
                            if (state.reason == LinkMemberFailure.SessionExpired) {
                                Res.string.profile_link_session_expired
                            } else {
                                Res.string.profile_link_failed
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onLoad) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }

                LinkMemberUiState.Linked -> CircularProgressIndicator()

                is LinkMemberUiState.AwaitingVerification -> CircularProgressIndicator()

                is LinkMemberUiState.Ready -> {
                    if (state.candidates.isEmpty()) {
                        Text(stringResource(Res.string.profile_link_candidates_empty))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.candidates, key = { it.id }) { candidate ->
                                val requesting = state.requestingCandidateId == candidate.id
                                SportsCard(
                                    onClick = { selected = candidate },
                                    enabled = state.requestingCandidateId == null,
                                    modifier = Modifier.fillMaxWidth(),
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
                                        if (requesting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp,
                                            )
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
            title = {
                Text(
                    stringResource(
                        Res.string.profile_link_guardian_title,
                        candidate.displayName,
                    ),
                )
            },
            text = {
                Text(stringResource(Res.string.profile_link_guardian_body))
            },
            confirmButton = {
                Button(
                    onClick = {
                        selected = null
                        onRequest(candidate, true)
                    },
                ) {
                    Text(stringResource(Res.string.profile_link_guardian_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selected = null
                        onRequest(candidate, false)
                    },
                ) {
                    Text(stringResource(Res.string.profile_link_guardian_no))
                }
            },
        )
    }

    (state as? LinkMemberUiState.AwaitingVerification)?.let { awaiting ->
        AlertDialog(
            onDismissRequest = onCancelVerification,
            title = { Text(awaiting.candidate.displayName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(Res.string.profile_link_code_body))
                    OutlinedTextField(
                        value = token,
                        onValueChange = {
                            token = it.filter(Char::isDigit).take(MEMBER_TOKEN_LENGTH)
                        },
                        label = { Text(stringResource(Res.string.profile_link_code)) },
                        singleLine = true,
                        enabled = !awaiting.verifying,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onVerify(token) },
                    enabled = token.length == MEMBER_TOKEN_LENGTH && !awaiting.verifying,
                ) {
                    Text(stringResource(Res.string.profile_link_verify))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onCancelVerification,
                    enabled = !awaiting.verifying,
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

private const val MEMBER_TOKEN_LENGTH = 6
