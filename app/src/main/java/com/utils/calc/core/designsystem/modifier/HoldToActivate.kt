package com.utils.calc.core.designsystem.modifier

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Toque longo com limiar próprio. O onLongPress do sistema dispara em ~500 ms —
 * curto demais para um gesto que cancela o modo de emergência ou que abre o
 * cofre: precisa ser difícil de acontecer por acidente.
 */
fun Modifier.holdToActivate(
    enabled: Boolean = true,
    holdMillis: Long = DEFAULT_HOLD_MILLIS,
    onHold: () -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(holdMillis, onHold) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            // null = estourou o tempo com o dedo na tela. Qualquer outro valor
            // significa que soltou antes, ou que um filho consumiu o toque.
            val finishedEarly = withTimeoutOrNull(holdMillis) {
                waitForUpOrCancellation() != null
            }
            if (finishedEarly == null) {
                onHold()
                waitForUpOrCancellation()
            }
        }
    }
}

const val DEFAULT_HOLD_MILLIS = 3_000L
