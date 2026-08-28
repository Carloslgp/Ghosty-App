package com.utils.calc.feature.calculator

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.utils.calc.R
import com.utils.calc.core.designsystem.modifier.DEFAULT_HOLD_MILLIS
import com.utils.calc.core.designsystem.modifier.rememberHoldGate
import com.utils.calc.core.designsystem.theme.AppTheme
import com.utils.calc.core.designsystem.theme.KeyLabelStyle
import com.utils.calc.core.designsystem.theme.Sizes
import com.utils.calc.feature.calculator.engine.CalcKey
import com.utils.calc.feature.calculator.engine.CalcOperator

@Composable
fun CalculatorKeypad(
    clearsEntryOnly: Boolean,
    longPressTriggerArmed: Boolean,
    onKey: (CalcKey) -> Unit,
    onZeroLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = AppTheme.extra
    val digitColor = extra.keypadDigit
    val functionColor = extra.keypadFunction
    val operatorColor = MaterialTheme.colorScheme.primaryContainer
    val onOperator = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Sizes.keypadSpacing),
    ) {
        KeypadRow {
            KeyButton(
                label = stringResource(
                    if (clearsEntryOnly) R.string.calc_clear_entry else R.string.calc_clear_all,
                ),
                container = functionColor,
                onClick = { onKey(CalcKey.Clear) },
            )
            KeyButton(
                label = stringResource(R.string.calc_sign),
                container = functionColor,
                onClick = { onKey(CalcKey.ToggleSign) },
            )
            KeyButton(
                label = stringResource(R.string.calc_percent),
                container = functionColor,
                onClick = { onKey(CalcKey.Percent) },
            )
            KeyButton(
                label = stringResource(R.string.calc_divide),
                container = operatorColor,
                contentColor = onOperator,
                onClick = { onKey(CalcKey.Operation(CalcOperator.Divide)) },
            )
        }

        KeypadRow {
            DigitKey(7, digitColor, onKey)
            DigitKey(8, digitColor, onKey)
            DigitKey(9, digitColor, onKey)
            KeyButton(
                label = stringResource(R.string.calc_multiply),
                container = operatorColor,
                contentColor = onOperator,
                onClick = { onKey(CalcKey.Operation(CalcOperator.Multiply)) },
            )
        }

        KeypadRow {
            DigitKey(4, digitColor, onKey)
            DigitKey(5, digitColor, onKey)
            DigitKey(6, digitColor, onKey)
            KeyButton(
                label = stringResource(R.string.calc_subtract),
                container = operatorColor,
                contentColor = onOperator,
                onClick = { onKey(CalcKey.Operation(CalcOperator.Subtract)) },
            )
        }

        KeypadRow {
            DigitKey(1, digitColor, onKey)
            DigitKey(2, digitColor, onKey)
            DigitKey(3, digitColor, onKey)
            KeyButton(
                label = stringResource(R.string.calc_add),
                container = operatorColor,
                contentColor = onOperator,
                onClick = { onKey(CalcKey.Operation(CalcOperator.Add)) },
            )
        }

        KeypadRow {
            ZeroKey(
                container = digitColor,
                longPressArmed = longPressTriggerArmed,
                onKey = onKey,
                onLongPress = onZeroLongPress,
            )
            KeyButton(
                label = stringResource(R.string.calc_decimal),
                container = digitColor,
                onClick = { onKey(CalcKey.Decimal) },
            )
            KeyButton(
                label = "",
                container = functionColor,
                icon = true,
                contentDescription = stringResource(R.string.calc_backspace),
                onClick = { onKey(CalcKey.Backspace) },
            )
            KeyButton(
                label = stringResource(R.string.calc_equals),
                container = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { onKey(CalcKey.Equals) },
            )
        }
    }
}

@Composable
private fun ColumnScope.KeypadRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(Sizes.keypadSpacing),
        content = content,
    )
}

@Composable
private fun RowScope.DigitKey(
    value: Int,
    container: Color,
    onKey: (CalcKey) -> Unit,
) {
    KeyButton(
        label = value.toString(),
        container = container,
        onClick = { onKey(CalcKey.Digit(value)) },
    )
}

/**
 * A tecla zero acumula duas funções: dígito no toque curto e gatilho de
 * emergência quando segurada por 3 s. O limiar longo é proposital — 500 ms
 * dispararia sozinho toda vez que ela digitasse devagar.
 */
@Composable
private fun RowScope.ZeroKey(
    container: Color,
    longPressArmed: Boolean,
    onKey: (CalcKey) -> Unit,
    onLongPress: () -> Unit,
) {
    val gate = rememberHoldGate(
        enabled = true,
        holdMillis = DEFAULT_HOLD_MILLIS,
        onHold = onLongPress,
    )
    KeyButton(
        label = "0",
        container = container,
        contentDescription = stringResource(R.string.calc_zero_key_description),
        interactionSource = gate.interactionSource,
        onClick = { if (!gate.shouldIgnoreClick()) onKey(CalcKey.Digit(0)) },
    )
}

@Composable
private fun RowScope.KeyButton(
    label: String,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = null,
    icon: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    val description = contentDescription ?: stringResource(R.string.calc_key_description, label)
    Surface(
        onClick = onClick,
        modifier = modifier
            .weight(1f)
            .fillMaxSize()
            .heightIn(min = Sizes.minTouchTarget)
            .semantics { this.contentDescription = description },
        shape = RoundedCornerShape(22.dp),
        color = container,
        contentColor = contentColor,
        interactionSource = interactionSource,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = null,
                )
            } else {
                Text(text = label, style = KeyLabelStyle)
            }
        }
    }
}
