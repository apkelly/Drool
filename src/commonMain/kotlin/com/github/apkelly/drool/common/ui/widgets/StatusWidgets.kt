package com.github.apkelly.drool.common.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.common.domain.model.RefreshFailure
import com.github.apkelly.drool.common.resources.Res
import com.github.apkelly.drool.common.resources.action_refresh
import com.github.apkelly.drool.common.resources.action_retry
import com.github.apkelly.drool.common.resources.cache_stale
import com.github.apkelly.drool.common.resources.offline_cached
import com.github.apkelly.drool.common.resources.refresh_failed
import com.github.apkelly.drool.common.resources.refresh_failed_empty
import com.github.apkelly.drool.common.ui.platform.showToolbarRefreshAction
import org.jetbrains.compose.resources.stringResource

@Composable
fun RefreshAction(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = showToolbarRefreshAction(),
) {
    if (!visible) return
    if (refreshing) {
        CircularProgressIndicator(modifier = modifier.padding(12.dp))
    } else {
        IconButton(onClick = onRefresh, modifier = modifier) {
            MaterialSymbolIcon(
                symbol = MaterialSymbol.Refresh,
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
    modifier: Modifier = Modifier,
) {
    if (!isStale && failure == null) return
    val offline = failure == RefreshFailure.Offline
    SportsCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (offline) {
                MaterialSymbolIcon(MaterialSymbol.CloudOff, contentDescription = null)
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
