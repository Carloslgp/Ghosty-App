package com.utils.calc.feature.calculator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.utils.calc.core.designsystem.theme.CalcTheme
import com.utils.calc.feature.calculator.engine.CalcState
import com.utils.calc.feature.calculator.engine.CalculatorEngine
import org.junit.Rule
import org.junit.Test

/**
 * Exercita a fachada pela interface: o teclado precisa somar de verdade e a
 * tecla de limpeza precisa alternar entre C e AC como em qualquer calculadora.
 */
class CalculatorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent() {
        composeRule.setContent {
            var state by androidx.compose.runtime.remember { mutableStateOf(CalcState()) }
            CalcTheme {
                CalculatorScreen(
                    state = CalculatorUiState(
                        display = CalculatorEngine.displayText(state),
                        expression = state.expression,
                        hasError = state.error != null,
                        clearsEntryOnly = state.clearsEntryOnly,
                        history = state.history,
                    ),
                    onKey = { state = CalculatorEngine.press(state, it) },
                    onZeroLongPress = {},
                    onClearHistory = { state = state.copy(history = emptyList()) },
                )
            }
        }
    }

    /** A tecla zero tem descrição própria porque acumula o gatilho de toque longo. */
    private fun tapKey(label: String) {
        val description = if (label == "0") "Tecla zero" else "Tecla $label"
        composeRule.onNodeWithContentDescription(description).performClick()
    }

    @Test
    fun somaPeloTeclado() {
        setContent()

        tapKey("1")
        tapKey("2")
        tapKey("+")
        tapKey("3")
        tapKey("0")
        tapKey("=")

        composeRule.onNodeWithText("42").assertIsDisplayed()
    }

    @Test
    fun teclaDeLimpezaAlternaEntreCeAC() {
        setContent()

        composeRule.onNodeWithContentDescription("Tecla AC").assertIsDisplayed()
        tapKey("7")
        composeRule.onNodeWithContentDescription("Tecla C").assertIsDisplayed()
        tapKey("C")
        composeRule.onNodeWithContentDescription("Tecla AC").assertIsDisplayed()
        composeRule.onNodeWithText("0").assertIsDisplayed()
    }

    @Test
    fun divisaoPorZeroMostraMensagem() {
        setContent()

        tapKey("8")
        tapKey("÷")
        tapKey("0")
        tapKey("=")

        composeRule.onNodeWithText("Não é possível dividir por zero").assertIsDisplayed()
    }
}
