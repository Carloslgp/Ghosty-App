package com.utils.calc.core.designsystem.modifier

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Permite que a mesma tecla responda a toque curto e a toque longo com limiar
 * proprio, sem perder o feedback visual do clique. A tecla "0" da calculadora
 * usa isso: toque some numero, toque longo de 3 s dispara o gatilho.
 */
@Stable
class HoldGate internal constructor(
    val interactionSource: MutableInteractionSource,
) {
    internal var triggered by mutableStateOf(false)
    internal var progressState = mutableFloatStateOf(0f)

    val progress: Float
        get() = progressState.floatValue

    /** true quando o toque ja' foi consumido pelo gesto longo. */
    fun shouldIgnoreClick(): Boolean = triggered
}

@Composable
fun rememberHoldGate(
    enabled: Boolean,
    holdMillis: Long = DEFAULT_HOLD_MILLIS,
    onHold: () -> Unit,
): HoldGate {
    val gate = remember { HoldGate(MutableInteractionSource()) }
    val currentOnHold by rememberUpdatedState(onHold)

    LaunchedEffect(gate, enabled, holdMillis) {
        var job: Job? = null
        gate.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    gate.triggered = false
                    gate.progressState.floatValue = 0f
                    if (enabled) {
                        job = launch {
                            val start = withFrameMillis { it }
                            var fraction = 0f
                            while (fraction < 1f) {
                                val now = withFrameMillis { it }
                                fraction = ((now - start).toFloat() / holdMillis).coerceIn(0f, 1f)
                                gate.progressState.floatValue = fraction
                            }
                            gate.triggered = true
                            currentOnHold()
                        }
                    }
                }

                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    job?.cancel()
                    job = null
                    gate.progressState.floatValue = 0f
                }
            }
        }
    }
    return gate
}
