package com.utils.calc.feature.vault.triggers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utils.calc.R
import com.utils.calc.core.designsystem.component.InfoNote
import com.utils.calc.core.designsystem.component.NoteTone
import com.utils.calc.core.designsystem.component.SectionCard
import com.utils.calc.core.designsystem.component.VaultScaffold
import com.utils.calc.core.designsystem.theme.Spacing
import com.utils.calc.core.domain.model.PanicDisguise
import com.utils.calc.core.domain.model.TriggerConfig
import com.utils.calc.core.domain.model.TriggerType

@Composable
fun TriggersRoute(
    onBack: () -> Unit,
    onQuickExit: () -> Unit,
    viewModel: TriggersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TriggersScreen(
        state = state,
        onBack = onBack,
        onQuickExit = {
            viewModel.onCloseSession()
            onQuickExit()
        },
        onToggle = viewModel::onToggle,
        onSensitivity = viewModel::onSensitivity,
        onPanicCodeChanged = viewModel::onPanicCodeChanged,
        onCountdownChanged = viewModel::onCountdownChanged,
        onDisguiseChanged = viewModel::onDisguiseChanged,
        onDuressAlertChanged = viewModel::onDuressAlertChanged,
    )
}

@Composable
fun TriggersScreen(
    state: TriggersUiState,
    onBack: () -> Unit,
    onQuickExit: () -> Unit,
    onToggle: (TriggerType, Boolean) -> Unit,
    onSensitivity: (TriggerType, Int) -> Unit,
    onPanicCodeChanged: (String) -> Unit,
    onCountdownChanged: (Int) -> Unit,
    onDisguiseChanged: (PanicDisguise) -> Unit,
    onDuressAlertChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    VaultScaffold(
        title = stringResource(R.string.triggers_title),
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
            SectionCard {
                state.settings.triggers.forEachIndexed { index, config ->
                    if (index > 0) HorizontalDivider()
                    TriggerRow(
                        config = config,
                        onToggle = { onToggle(config.type, it) },
                        onSensitivity = { onSensitivity(config.type, it) },
                    )
                }
            }

            SectionCard(
                title = stringResource(R.string.triggers_code_title),
                subtitle = stringResource(R.string.triggers_code_help),
            ) {
                OutlinedTextField(
                    value = state.settings.panicCode,
                    onValueChange = onPanicCodeChanged,
                    singleLine = true,
                    isError = state.codeProblem != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    supportingText = {
                        state.codeProblem?.let { problem ->
                            Text(
                                stringResource(
                                    when (problem) {
                                        PanicCodeProblem.TooShort ->
                                            R.string.triggers_code_error_short

                                        PanicCodeProblem.Conflicts ->
                                            R.string.triggers_code_error_conflict
                                    },
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionCard(
                title = stringResource(R.string.triggers_countdown_title),
                subtitle = stringResource(R.string.triggers_countdown_help),
            ) {
                Text(
                    text = if (state.settings.countdownSeconds == 0) {
                        stringResource(R.string.triggers_countdown_immediate)
                    } else {
                        pluralStringResource(
                            R.plurals.triggers_countdown_value,
                            state.settings.countdownSeconds,
                            state.settings.countdownSeconds,
                        )
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = state.settings.countdownSeconds.toFloat(),
                    onValueChange = { onCountdownChanged(it.toInt()) },
                    valueRange = 0f..30f,
                    steps = 29,
                )
            }

            SectionCard(title = stringResource(R.string.triggers_disguise_title)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PanicDisguise.entries.forEach { disguise ->
                        FilterChip(
                            selected = state.settings.disguise == disguise,
                            onClick = { onDisguiseChanged(disguise) },
                            label = { Text(stringResource(disguise.labelRes())) },
                        )
                    }
                }
            }

            SectionCard(title = stringResource(R.string.triggers_duress_alert_title)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Text(
                        text = stringResource(R.string.triggers_duress_alert_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.settings.duressPinAlsoAlerts,
                        onCheckedChange = onDuressAlertChanged,
                    )
                }
            }

            InfoNote(
                text = stringResource(R.string.triggers_phase_notice),
                tone = NoteTone.Warning,
            )
        }
    }
}

@Composable
private fun TriggerRow(
    config: TriggerConfig,
    onToggle: (Boolean) -> Unit,
    onSensitivity: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = stringResource(config.type.titleRes()),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (!config.available) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(stringResource(R.string.trigger_unavailable)) },
                        )
                    }
                }
                Text(
                    text = stringResource(config.type.descriptionRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = config.enabled && config.available,
                enabled = config.available,
                onCheckedChange = onToggle,
            )
        }

        if (config.available && config.enabled && config.type.usesSensitivity()) {
            Text(
                text = stringResource(R.string.trigger_sensitivity, config.sensitivity),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = config.sensitivity.toFloat(),
                onValueChange = { onSensitivity(it.toInt()) },
                valueRange = 0f..100f,
            )
        }
    }
}

private fun TriggerType.usesSensitivity(): Boolean =
    this == TriggerType.SHAKE || this == TriggerType.POWER_BUTTON

private fun TriggerType.titleRes(): Int = when (this) {
    TriggerType.CALCULATOR_CODE -> R.string.trigger_calculator_code
    TriggerType.KEY_LONG_PRESS -> R.string.trigger_long_press
    TriggerType.QUICK_SETTINGS_TILE -> R.string.trigger_tile
    TriggerType.POWER_BUTTON -> R.string.trigger_power
    TriggerType.VOLUME_KEYS -> R.string.trigger_volume
    TriggerType.SHAKE -> R.string.trigger_shake
}

private fun TriggerType.descriptionRes(): Int = when (this) {
    TriggerType.CALCULATOR_CODE -> R.string.trigger_calculator_code_desc
    TriggerType.KEY_LONG_PRESS -> R.string.trigger_long_press_desc
    TriggerType.QUICK_SETTINGS_TILE -> R.string.trigger_tile_desc
    TriggerType.POWER_BUTTON -> R.string.trigger_power_desc
    TriggerType.VOLUME_KEYS -> R.string.trigger_volume_desc
    TriggerType.SHAKE -> R.string.trigger_shake_desc
}

private fun PanicDisguise.labelRes(): Int = when (this) {
    PanicDisguise.CALCULATOR -> R.string.triggers_disguise_calculator
    PanicDisguise.BLACK_SCREEN -> R.string.triggers_disguise_black
    PanicDisguise.LOW_BATTERY -> R.string.triggers_disguise_battery
}
