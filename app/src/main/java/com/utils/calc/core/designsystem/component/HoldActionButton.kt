package com.utils.calc.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.utils.calc.core.designsystem.modifier.rememberHoldGate
import com.utils.calc.core.designsystem.theme.Sizes
import com.utils.calc.core.designsystem.theme.Spacing

/**
 * Botao que so' aciona depois de segurar. Um botao de panico que dispara no
 * toque dispara sozinho dentro da bolsa; um que exige segurar, nao.
 */
@Composable
fun HoldActionButton(
    label: String,
    hint: String,
    holdMillis: Long,
    containerColor: Color,
    contentColor: Color,
    onActivated: () -> Unit,
    modifier: Modifier = Modifier,
    onShortTap: () -> Unit = {},
    enabled: Boolean = true,
    accessibilityLabel: String = label,
) {
    val gate = rememberHoldGate(enabled = enabled, holdMillis = holdMillis, onHold = onActivated)
    val progress by animateFloatAsState(targetValue = gate.progress, label = "holdProgress")

    Box(
        modifier = modifier
            .size(Sizes.panicButton)
            .semantics { contentDescription = accessibilityLabel },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = gate.interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = { if (!gate.shouldIgnoreClick()) onShortTap() },
                ),
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            if (progress > 0f) {
                drawArc(
                    color = contentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
    }
}
