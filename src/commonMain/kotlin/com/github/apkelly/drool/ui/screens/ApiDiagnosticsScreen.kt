package com.github.apkelly.drool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.action_back
import com.github.apkelly.drool.resources.api_diagnostics_clubs
import com.github.apkelly.drool.resources.api_diagnostics_description
import com.github.apkelly.drool.resources.api_diagnostics_linked_users
import com.github.apkelly.drool.resources.api_diagnostics_schedule
import com.github.apkelly.drool.resources.api_diagnostics_teams
import com.github.apkelly.drool.resources.api_diagnostics_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun ApiDiagnosticsScreen(
    onBack: () -> Unit,
    onLinkedUsers: () -> Unit,
    onClubs: () -> Unit,
    onTeams: () -> Unit,
    onSchedule: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) {
                Text(stringResource(Res.string.action_back))
            }
        }
        item {
            Text(
                stringResource(Res.string.api_diagnostics_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            Text(stringResource(Res.string.api_diagnostics_description))
        }
        item {
            DiagnosticButton(
                label = stringResource(Res.string.api_diagnostics_linked_users),
                onClick = onLinkedUsers,
            )
        }
        item {
            DiagnosticButton(
                label = stringResource(Res.string.api_diagnostics_clubs),
                onClick = onClubs,
            )
        }
        item {
            DiagnosticButton(
                label = stringResource(Res.string.api_diagnostics_teams),
                onClick = onTeams,
            )
        }
        item {
            DiagnosticButton(
                label = stringResource(Res.string.api_diagnostics_schedule),
                onClick = onSchedule,
            )
        }
    }
}

@Composable
private fun DiagnosticButton(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label)
    }
}
