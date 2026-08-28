package com.utils.calc.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.utils.calc.R
import com.utils.calc.core.designsystem.theme.Spacing

/**
 * Teclado numérico das telas de código. Separado do teclado da calculadora de
 * propósito: aqui não existe operador, e a tecla apagar precisa ser grande —
 * quem digita um código sob pressão erra.
 */
@Composable
fun NumericKeypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        listOf(1..3, 4..6, 7..9).forEach { range ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                range.forEach { digit -> PadKey(digit.toString()) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Box(modifier = Modifier.weight(1f))
            PadKey("0") { onDigit(0) }
            PadKey(
                label = "",
                icon = true,
                description = stringResource(R.string.calc_backspace),
                onClick = onBackspace,
            )
        }
    }
}

@Composable
private fun RowScope.PadKey(
    label: String,
    icon: Boolean = false,
    description: String? = null,
    onClick: () -> Unit,
) {
    val contentDescription = description ?: label
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(64.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = null,
                )
            } else {
                Text(text = label, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
