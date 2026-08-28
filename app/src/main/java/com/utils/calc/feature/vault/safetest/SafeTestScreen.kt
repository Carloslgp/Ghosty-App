package com.utils.calc.feature.vault.safetest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utils.calc.R
import com.utils.calc.core.designsystem.component.InfoNote
import com.utils.calc.core.designsystem.component.NoteTone
import com.utils.calc.core.designsystem.component.SectionCard
import com.utils.calc.core.designsystem.component.VaultScaffold
import com.utils.calc.core.designsystem.theme.Spacing
import com.utils.calc.core.domain.model.PanicDisguise
import com.utils.calc.feature.panic.PanicOverlay
import com.utils.calc.feature.panic.disguiseLabel
import com.utils.calc.feature.panic.formatClock
import com.utils.calc.feature.panic.panicEventText

@Composable
fun SafeTestRoute(
    onBack: () -> Unit,
    onQuickExit: () -> Unit,
    viewModel: SafeTestViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SafeTestScreen(
        state = state,
        onRun = viewModel::onRun,
        onBack = onBack,
        onQuickExit = {
            viewModel.onCloseSession()
            onQuickExit()
        },
    )
}

@Composable
fun SafeTestScreen(
    state: SafeTestUiState,
    onRun: () -> Unit,
    onBack: () -> Unit,
    onQuickExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var previewing by remember { mutableStateOf<PanicDisguise?>(null) }

    VaultScaffold(
        title = stringResource(R.string.safe_test_title),
        onQuickExit = onQuickExit,
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            InfoNote(text = stringResource(R.string.safe_test_intro))

            if (!state.hasContacts) {
                InfoNote(
                    text = stringResource(R.string.safe_test_no_contacts),
                    tone = NoteTone.Warning,
                )
            }

            Button(
                onClick = onRun,
                enabled = !state.running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = stringResource(R.string.safe_test_running),
                        modifier = Modifier.padding(start = Spacing.sm),
                    )
                } else {
                    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null)
                    Text(
                        text = stringResource(
                            if (state.finished) R.string.safe_test_again else R.string.safe_test_run,
                        ),
                        modifier = Modifier.padding(start = Spacing.sm),
                    )
                }
            }

            if (state.log.isNotEmpty()) {
                SectionCard(title = stringResource(R.string.safe_test_result_title)) {
                    state.log.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalAlignment = Alignment.Top,
                        ) {
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

            SectionCard(
                title = stringResource(R.string.safe_test_preview_title),
                subtitle = stringResource(R.string.safe_test_preview_body),
            ) {
                PanicDisguise.entries.forEach { disguise ->
                    OutlinedButton(
                        onClick = { previewing = disguise },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                R.string.safe_test_preview_open,
                                disguiseLabel(disguise),
                            ),
                        )
                    }
                }
            }
        }
    }

    previewing?.let { disguise ->
        Dialog(
            onDismissRequest = { previewing = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
        ) {
            PanicOverlay(disguise = disguise, onCancel = { previewing = null })
        }
    }
}
