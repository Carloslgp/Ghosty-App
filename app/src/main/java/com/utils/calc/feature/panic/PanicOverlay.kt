package com.utils.calc.feature.panic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.utils.calc.R
import com.utils.calc.core.designsystem.component.SecureScreen
import com.utils.calc.core.designsystem.modifier.DEFAULT_HOLD_MILLIS
import com.utils.calc.core.designsystem.modifier.holdToActivate
import com.utils.calc.core.designsystem.theme.CalculatorDisplayStyle
import com.utils.calc.core.designsystem.theme.Spacing
import com.utils.calc.core.domain.model.PanicDisguise
import com.utils.calc.feature.calculator.CalculatorKeypad
import com.utils.calc.feature.calculator.engine.CalcState
import com.utils.calc.feature.calculator.engine.CalculatorEngine

/**
 * O que fica na tela durante a emergência. Nenhuma das três aparências revela
 * que algo está acontecendo — quem estiver olhando por cima do ombro precisa ver
 * um aparelho comum. A única saída é o toque longo de 3 s, em qualquer ponto.
 */
@Composable
fun PanicOverlay(
    disguise: PanicDisguise,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SecureScreen()

    // Voltar não pode fechar o disfarce: fecharia justamente quando o aparelho
    // estiver na mão de outra pessoa.
    BackHandler(enabled = true) { }

    Box(
        modifier = modifier
            .fillMaxSize()
            .holdToActivate(holdMillis = DEFAULT_HOLD_MILLIS, onHold = onCancel),
    ) {
        when (disguise) {
            PanicDisguise.CALCULATOR -> CalculatorDisguise()
            PanicDisguise.BLACK_SCREEN -> BlackScreenDisguise()
            PanicDisguise.LOW_BATTERY -> LowBatteryDisguise()
        }
    }
}

/**
 * Calculadora completa e funcional. Uma casca que não calcula denuncia o app no
 * primeiro toque de quem estiver segurando o aparelho.
 */
@Composable
private fun CalculatorDisguise() {
    var state by remember { mutableStateOf(CalcState()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Text(
                text = if (state.error != null) {
                    stringResource(R.string.calc_error_divide_by_zero)
                } else {
                    CalculatorEngine.displayText(state)
                },
                style = CalculatorDisplayStyle,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
        }

        CalculatorKeypad(
            clearsEntryOnly = state.clearsEntryOnly,
            longPressTriggerArmed = false,
            onKey = { key -> state = CalculatorEngine.press(state, key) },
            onZeroLongPress = { },
            modifier = Modifier.weight(2.6f),
        )
    }
}

@Composable
private fun BlackScreenDisguise() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    )
}

/**
 * Aviso de bateria fraca. O "OK" apaga a tela em vez de sair — é o
 * comportamento que a pessoa do outro lado espera de um aparelho acabando.
 */
@Composable
private fun LowBatteryDisguise() {
    var dismissed by remember { mutableStateOf(false) }

    if (dismissed) {
        BlackScreenDisguise()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                imageVector = Icons.Outlined.BatteryAlert,
                contentDescription = null,
                tint = Color(0xFFB0B4BA),
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = stringResource(R.string.panic_low_battery_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFE8EAEE),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.panic_low_battery_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0B4BA),
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = { dismissed = true }) {
                Text(
                    text = stringResource(R.string.panic_low_battery_dismiss),
                    color = Color(0xFF9AB6F0),
                )
            }
        }
    }
}
