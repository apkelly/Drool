package com.github.apkelly.drool.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.domain.model.RefreshFailure
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.action_refresh
import com.github.apkelly.drool.resources.action_retry
import com.github.apkelly.drool.resources.cache_stale
import com.github.apkelly.drool.resources.offline_cached
import com.github.apkelly.drool.resources.refresh_failed
import com.github.apkelly.drool.resources.refresh_failed_empty
import org.jetbrains.compose.resources.stringResource

@Composable
fun RefreshAction(
    refreshing: Boolean,
    onRefresh: () -> Unit,
) {
    if (refreshing) {
        CircularProgressIndicator(modifier = Modifier.padding(12.dp))
    } else {
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(Res.string.action_refresh),
            )
        }
    }
}

@Composable
fun CacheStatus(
    isStale: Boolean,
    failure: RefreshFailure?,
    hasContent: Boolean,
    onRetry: () -> Unit,
) {
    if (!isStale && failure == null) return
    val offline = failure == RefreshFailure.Offline
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (offline) {
                Icon(Icons.Default.CloudOff, contentDescription = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        offline -> stringResource(Res.string.offline_cached)
                        failure != null && hasContent -> stringResource(Res.string.refresh_failed)
                        failure != null -> stringResource(Res.string.refresh_failed_empty)
                        else -> stringResource(Res.string.cache_stale)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (failure != null) {
                Button(onClick = onRetry) {
                    Text(stringResource(Res.string.action_retry))
                }
            }
        }
    }
}
