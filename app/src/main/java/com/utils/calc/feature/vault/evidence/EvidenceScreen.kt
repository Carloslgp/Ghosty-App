package com.utils.calc.feature.vault.evidence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utils.calc.R
import com.utils.calc.core.designsystem.component.EmptyState
import com.utils.calc.core.designsystem.component.InfoNote
import com.utils.calc.core.designsystem.component.SectionCard
import com.utils.calc.core.designsystem.component.VaultScaffold
import com.utils.calc.core.designsystem.theme.Spacing
import com.utils.calc.core.domain.model.EvidenceItem
import com.utils.calc.core.domain.model.EvidenceKind
import com.utils.calc.feature.panic.formatBytes
import com.utils.calc.feature.panic.formatCoordinates
import com.utils.calc.feature.panic.formatDateTime
import com.utils.calc.feature.panic.formatDuration

@Composable
fun EvidenceRoute(
    onBack: () -> Unit,
    onQuickExit: () -> Unit,
    viewModel: EvidenceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    EvidenceScreen(
        state = state,
        onBack = onBack,
        onQuickExit = {
            viewModel.onCloseSession()
            onQuickExit()
        },
        onTogglePlayback = viewModel::onTogglePlayback,
        onDelete = viewModel::onDelete,
        onDeleteAll = viewModel::onDeleteAll,
    )
}

@Composable
fun EvidenceScreen(
    state: EvidenceUiState,
    onBack: () -> Unit,
    onQuickExit: () -> Unit,
    onTogglePlayback: (EvidenceItem) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<EvidenceItem?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    VaultScaffold(
        title = stringResource(R.string.evidence_title),
        onQuickExit = onQuickExit,
        onBack = onBack,
        modifier = modifier,
        actions = {
            if (state.items.isNotEmpty()) {
                TextButton(onClick = { confirmDeleteAll = true }) {
                    Text(stringResource(R.string.evidence_delete_all))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.items.isEmpty()) {
                SectionCard {
                    EmptyState(
                        icon = Icons.Outlined.Mic,
                        title = stringResource(R.string.evidence_empty_title),
                        description = stringResource(R.string.evidence_empty_body),
                    )
                }
            } else {
                SectionCard {
                    state.items.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider()
                        EvidenceRow(
                            item = item,
                            playing = state.playback.itemId == item.id,
                            position = state.playback.positionMillis,
                            onTogglePlayback = { onTogglePlayback(item) },
                            onDelete = { pendingDelete = item },
                        )
                    }
                }
                InfoNote(text = stringResource(R.string.evidence_placeholder))
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.evidence_delete_title)) },
            text = { Text(stringResource(R.string.evidence_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(item.id)
                        pendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text(stringResource(R.string.evidence_delete_all)) },
            text = { Text(stringResource(R.string.evidence_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAll()
                        confirmDeleteAll = false
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun EvidenceRow(
    item: EvidenceItem,
    playing: Boolean,
    position: Long,
    onTogglePlayback: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(item.kind.labelRes()) + " · " +
                        formatDateTime(item.createdAt),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(
                        R.string.evidence_duration,
                        formatDuration(item.durationMillis),
                    ) + " · " + formatBytes(item.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.location?.let { point ->
                    Text(
                        text = formatCoordinates(point.latitude, point.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onTogglePlayback) {
                Icon(
                    imageVector = if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(
                        if (playing) R.string.evidence_stop else R.string.evidence_play,
                    ),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                )
            }
        }

        if (playing && item.durationMillis > 0) {
            LinearProgressIndicator(
                progress = { position.toFloat() / item.durationMillis.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs),
            )
        }
    }
}

private fun EvidenceKind.labelRes(): Int = when (this) {
    EvidenceKind.AUDIO -> R.string.evidence_kind_audio
    EvidenceKind.VIDEO -> R.string.evidence_kind_video
    EvidenceKind.LOCATION -> R.string.evidence_kind_location
}
