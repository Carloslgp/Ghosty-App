package com.utils.calc.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.utils.calc.core.designsystem.theme.Spacing

@Composable
fun PinDots(
    filled: Int,
    total: Int,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    accessibilityLabel: String = "",
) {
    val activeColor = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.semantics { contentDescription = accessibilityLabel },
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val isFilled = index < filled
            val size by animateDpAsState(if (isFilled) 16.dp else 12.dp, label = "pinDot")
            Row(
                modifier = Modifier
                    .size(size)
                    .background(
                        color = if (isFilled) activeColor else Color.Transparent,
                        shape = CircleShape,
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isFilled) activeColor else MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    ),
            ) {}
        }
    }
}
