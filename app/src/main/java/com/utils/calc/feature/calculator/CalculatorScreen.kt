package com.utils.calc.feature.calculator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utils.calc.R
import com.utils.calc.core.designsystem.component.InfoNote
import com.utils.calc.core.designsystem.theme.CalculatorDisplayStyle
import com.utils.calc.core.designsystem.theme.CalculatorSecondaryStyle
import com.utils.calc.core.designsystem.theme.Spacing
import com.utils.calc.feature.calculator.engine.CalcKey
import com.utils.calc.feature.calculator.engine.CalculationRecord

@Composable
fun CalculatorRoute(
    onOpenOnboarding: () -> Unit,
    onOpenVault: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                CalculatorEvent.OpenOnboarding -> onOpenOnboarding()
                CalculatorEvent.OpenVault -> onOpenVault()
            }
        }
    }

    CalculatorScreen(
        state = state,
        onKey = viewModel::onKey,
        onZeroLongPress = viewModel::onZeroLongPress,
        onClearHistory = viewModel::onClearHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    onKey: (CalcKey) -> Unit,
    onZeroLongPress: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showHistory by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { showHistory = true }) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = stringResource(R.string.calc_history_open),
                )
            }
        }

        if (state.showSetupHint) {
            InfoNote(
                title = stringResource(R.string.bootstrap_hint_title),
                text = stringResource(R.string.bootstrap_hint_body, state.setupCode),
                modifier = Modifier.padding(bottom = Spacing.sm),
            )
        }

        Display(state = state, modifier = Modifier.weight(1f))

        CalculatorKeypad(
            clearsEntryOnly = state.clearsEntryOnly,
            longPressTriggerArmed = state.longPressTriggerArmed,
            onKey = onKey,
            onZeroLongPress = onZeroLongPress,
            modifier = Modifier.weight(2.6f),
        )
    }

    if (showHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            HistorySheet(
                records = state.history,
                onClear = {
                    onClearHistory()
                    showHistory = false
                },
            )
        }
    }
}

@Composable
private fun Display(state: CalculatorUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.md),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = state.expression,
            style = CalculatorSecondaryStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            textAlign = TextAlign.End,
        )

        if (state.hasError) {
            Text(
                text = stringResource(R.string.calc_error_divide_by_zero),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        } else {
            Text(
                text = state.display,
                style = CalculatorDisplayStyle,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            )
        }
    }
}

@Composable
private fun HistorySheet(
    records: List<CalculationRecord>,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.sm,
            bottom = Spacing.lg,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.calc_history_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (records.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.calc_history_clear))
                }
            }
        }

        if (records.isEmpty()) {
            Text(
                text = stringResource(R.string.calc_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.lg),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(records, key = { it.id }) { record ->
                    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
                        Text(
                            text = record.expression,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = record.result,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }

}
