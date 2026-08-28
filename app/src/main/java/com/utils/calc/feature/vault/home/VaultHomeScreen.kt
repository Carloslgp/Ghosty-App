package com.utils.calc.feature.vault.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utils.calc.R
import com.utils.calc.core.designsystem.component.HoldActionButton
import com.utils.calc.core.designsystem.component.InfoNote
import com.utils.calc.core.designsystem.component.NoteTone
import com.utils.calc.core.designsystem.component.SectionCard
import com.utils.calc.core.designsystem.component.VaultScaffold
import com.utils.calc.core.designsystem.theme.AppTheme
import com.utils.calc.core.designsystem.theme.Spacing
import com.utils.calc.feature.panic.formatClock
import com.utils.calc.feature.panic.panicEventText

private const val PANIC_HOLD_MILLIS = 1_200L

@Composable
fun VaultHomeRoute(
    onQuickExit: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenTriggers: () -> Unit,
    onOpenEvidence: () -> Unit,
    onOpenSafeTest: () -> Unit,
    onResetCompleted: () -> Unit,
    viewModel: VaultHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    VaultHomeScreen(
        state = state,
        onPanic = viewModel::onPanic,
        onQuickExit = {
            viewModel.onCloseSession()
            onQuickExit()
        },
        onOpenContacts = onOpenContacts,
        onOpenTriggers = onOpenTriggers,
        onOpenEvidence = onOpenEvidence,
        onOpenSafeTest = onOpenSafeTest,
        onReset = {
            viewModel.onResetEverything()
            onResetCompleted()
        },
    )
}

@Composable
fun VaultHomeScreen(
    state: VaultHomeUiState,
    onPanic: () -> Unit,
    onQuickExit: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenTriggers: () -> Unit,
    onOpenEvidence: () -> Unit,
    onOpenSafeTest: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Voltar aqui nao volta para a calculadora "por tras": sai de vez e fecha a sessao.
    BackHandler { onQuickExit() }

    var menuOpen by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var showHoldHint by remember { mutableStateOf(false) }

    VaultScaffold(
        title = stringResource(R.string.vault_home_title),
        onQuickExit = onQuickExit,
        modifier = modifier,
        actions = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.settings_title),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_reset_title)) },
                        onClick = {
                            menuOpen = false
                            confirmReset = true
                        },
                    )
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
            if (state.isDuress) {
                InfoNote(
                    text = stringResource(R.string.vault_duress_notice),
                    tone = NoteTone.Warning,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                HoldActionButton(
                    label = stringResource(R.string.vault_panic_button),
                    hint = stringResource(R.string.vault_panic_hint),
                    holdMillis = PANIC_HOLD_MILLIS,
                    containerColor = AppTheme.extra.danger,
                    contentColor = AppTheme.extra.onDanger,
                    onActivated = onPanic,
                    onShortTap = { showHoldHint = true },
                    accessibilityLabel = stringResource(R.string.vault_panic_description),
                )
            }

            if (showHoldHint) {
                InfoNote(text = stringResource(R.string.vault_panic_short_tap))
            }

            if (!state.hasContacts) {
                InfoNote(
                    text = stringResource(R.string.vault_no_contacts_warning),
                    tone = NoteTone.Warning,
                )
            }

            SectionCard(title = stringResource(R.string.vault_section_status)) {
                StatusLine(
                    text = if (state.hasContacts) {
                        pluralStringResource(
                            R.plurals.vault_status_contacts,
                            state.contactCount,
                            state.contactCount,
                        )
                    } else {
                        stringResource(R.string.vault_status_no_contacts)
                    },
                )
                StatusLine(
                    text = pluralStringResource(
                        R.plurals.vault_status_triggers,
                        state.activeTriggerCount,
                        state.activeTriggerCount,
                    ),
                )
                StatusLine(
                    text = state.panicCode?.let {
                        stringResource(R.string.vault_status_code, it)
                    } ?: stringResource(R.string.vault_status_code_off),
                )
            }

            SectionCard(title = stringResource(R.string.vault_section_shortcuts)) {
                ShortcutRow(
                    icon = Icons.Outlined.Group,
                    title = stringResource(R.string.vault_contacts),
                    subtitle = stringResource(R.string.vault_contacts_subtitle),
                    onClick = onOpenContacts,
                )
                HorizontalDivider()
                ShortcutRow(
                    icon = Icons.Outlined.Bolt,
                    title = stringResource(R.string.vault_triggers),
                    subtitle = stringResource(R.string.vault_triggers_subtitle),
                    onClick = onOpenTriggers,
                )
                HorizontalDivider()
                ShortcutRow(
                    icon = Icons.Outlined.Mic,
                    title = stringResource(R.string.vault_evidence),
                    subtitle = stringResource(R.string.vault_evidence_subtitle),
                    onClick = onOpenEvidence,
                )
                HorizontalDivider()
                ShortcutRow(
                    icon = Icons.Outlined.Science,
                    title = stringResource(R.string.vault_safe_test),
                    subtitle = stringResource(R.string.vault_safe_test_subtitle),
                    onClick = onOpenSafeTest,
                )
            }

            SectionCard(title = stringResource(R.string.vault_last_run_title)) {
                if (state.lastRunLog.isEmpty()) {
                    Text(
                        text = stringResource(R.string.vault_last_run_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    if (state.lastRunSimulated) {
                        Text(
                            text = stringResource(R.string.vault_last_run_rehearsal),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.lastRunLog.forEach { entry ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(
                                text = formatClock(entry.at),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = panicEventText(entry.event),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.settings_reset_title)) },
            text = { Text(stringResource(R.string.settings_reset_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        onReset()
                    },
                ) {
                    Text(stringResource(R.string.settings_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun StatusLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
