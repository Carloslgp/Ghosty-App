package com.utils.calc.feature.calculator

import com.utils.calc.feature.calculator.engine.CalcError
import com.utils.calc.feature.calculator.engine.CalcKey
import com.utils.calc.feature.calculator.engine.CalcOperator
import com.utils.calc.feature.calculator.engine.CalcState
import com.utils.calc.feature.calculator.engine.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

    private fun run(vararg keys: CalcKey): CalcState =
        keys.fold(CalcState()) { state, key -> CalculatorEngine.press(state, key) }

    private fun digits(text: String): Array<CalcKey> =
        text.map { CalcKey.Digit(it.digitToInt()) }.toTypedArray()

    private fun display(state: CalcState) = CalculatorEngine.displayText(state)

    @Test
    fun `soma simples`() {
        val state = run(
            *digits("12"),
            CalcKey.Operation(CalcOperator.Add),
            *digits("5"),
            CalcKey.Equals,
        )
        assertEquals("17", display(state))
    }

    @Test
    fun `operacoes encadeadas avaliam da esquerda para a direita`() {
        val state = run(
            *digits("2"),
            CalcKey.Operation(CalcOperator.Add),
            *digits("3"),
            CalcKey.Operation(CalcOperator.Multiply),
            *digits("4"),
            CalcKey.Equals,
        )
        assertEquals("20", display(state))
    }

    @Test
    fun `operador mostra o resultado parcial antes de terminar a conta`() {
        val state = run(
            *digits("2"),
            CalcKey.Operation(CalcOperator.Add),
            *digits("3"),
            CalcKey.Operation(CalcOperator.Multiply),
        )
        assertEquals("5", display(state))
    }

    @Test
    fun `trocar de operador nao calcula nada`() {
        val state = run(
            *digits("8"),
            CalcKey.Operation(CalcOperator.Add),
            CalcKey.Operation(CalcOperator.Divide),
            *digits("2"),
            CalcKey.Equals,
        )
        assertEquals("4", display(state))
    }

    @Test
    fun `decimais nao acumulam erro de ponto flutuante`() {
        val state = run(
            *digits("0"),
            CalcKey.Decimal,
            *digits("1"),
            CalcKey.Operation(CalcOperator.Add),
            *digits("0"),
            CalcKey.Decimal,
            *digits("2"),
            CalcKey.Equals,
        )
        assertEquals("0,3", display(state))
    }

    @Test
    fun `divisao por zero vira erro e a proxima tecla recomeca`() {
        val error = run(
            *digits("9"),
            CalcKey.Operation(CalcOperator.Divide),
            *digits("0"),
            CalcKey.Equals,
        )
        assertEquals(CalcError.DivideByZero, error.error)

        val recovered = CalculatorEngine.press(error, CalcKey.Digit(7))
        assertNull(recovered.error)
        assertEquals("7", display(recovered))
    }

    @Test
    fun `porcentagem em soma e relativa ao primeiro operando`() {
        val state = run(
            *digits("50"),
            CalcKey.Operation(CalcOperator.Add),
            *digits("10"),
            CalcKey.Percent,
            CalcKey.Equals,
        )
        assertEquals("55", display(state))
    }

    @Test
    fun `porcentagem sozinha divide por cem`() {
        val state = run(*digits("50"), CalcKey.Percent)
        assertEquals("0,5", display(state))
    }

    @Test
    fun `porcentagem em multiplicacao divide por cem`() {
        val state = run(
            *digits("200"),
            CalcKey.Operation(CalcOperator.Multiply),
            *digits("10"),
            CalcKey.Percent,
            CalcKey.Equals,
        )
        assertEquals("20", display(state))
    }

    @Test
    fun `igual repetido repete a ultima operacao`() {
        var state = run(
            *digits("2"),
            CalcKey.Operation(CalcOperator.Add),
            *digits("3"),
            CalcKey.Equals,
        )
        assertEquals("5", display(state))
        state = CalculatorEngine.press(state, CalcKey.Equals)
        assertEquals("8", display(state))
        state = CalculatorEngine.press(state, CalcKey.Equals)
        assertEquals("11", display(state))
    }

    @Test
    fun `inverter sinal antes de digitar produz operando negativo`() {
        val state = run(
            *digits("10"),
            CalcKey.Operation(CalcOperator.Add),
            CalcKey.ToggleSign,
            *digits("4"),
            CalcKey.Equals,
        )
        assertEquals("6", display(state))
    }

    @Test
    fun `apagar remove um digito de cada vez`() {
        val state = run(*digits("123"), CalcKey.Backspace)
        assertEquals("12", display(state))
    }

    @Test
    fun `apagar em visor zerado mantem zero`() {
        val state = run(*digits("5"), CalcKey.Backspace, CalcKey.Backspace)
        assertEquals("0", display(state))
    }

    @Test
    fun `limpar apaga a entrada e depois a conta inteira`() {
        val typed = run(
            *digits("10"),
            CalcKey.Operation(CalcOperator.Add),
            *digits("7"),
        )
        assertTrue(typed.clearsEntryOnly)

        val clearedEntry = CalculatorEngine.press(typed, CalcKey.Clear)
        assertEquals("0", display(clearedEntry))
        assertEquals(CalcOperator.Add, clearedEntry.pendingOperator)

        val clearedAll = CalculatorEngine.press(clearedEntry, CalcKey.Clear)
        assertNull(clearedAll.pendingOperator)
        assertNull(clearedAll.accumulator)
        assertEquals("", clearedAll.expression)
    }

    @Test
    fun `visor agrupa milhares e usa virgula decimal`() {
        val state = run(*digits("1234567"), CalcKey.Decimal, *digits("89"))
        assertEquals("1.234.567,89", display(state))
    }

    @Test
    fun `entrada respeita o limite de digitos`() {
        val state = run(*digits("1234567890123456"))
        assertEquals(CalculatorEngine.MAX_DIGITS, state.entry.count { it.isDigit() })
    }

    @Test
    fun `resultado longo cai para notacao cientifica`() {
        val state = run(
            *digits("999999999999"),
            CalcKey.Operation(CalcOperator.Multiply),
            *digits("999999999999"),
            CalcKey.Equals,
        )
        assertTrue(display(state).contains("E+"))
    }

    @Test
    fun `divisao inexata arredonda para doze digitos significativos`() {
        val state = run(
            *digits("1"),
            CalcKey.Operation(CalcOperator.Divide),
            *digits("3"),
            CalcKey.Equals,
        )
        assertEquals("0,333333333333", display(state))
    }

    @Test
    fun `historico guarda a conta concluida`() {
        val state = run(
            *digits("7"),
            CalcKey.Operation(CalcOperator.Multiply),
            *digits("6"),
            CalcKey.Equals,
        )
        assertEquals(1, state.history.size)
        assertEquals("42", state.history.first().result)
        assertEquals("7 × 6 =", state.history.first().expression)
    }

    @Test
    fun `codigo secreto so vale com digitos limpos`() {
        assertTrue(run(*digits("1984")).isCleanCodeCandidate)
        assertFalse(run(*digits("1984"), CalcKey.Equals).isCleanCodeCandidate)
        assertFalse(
            run(
                *digits("1000"),
                CalcKey.Operation(CalcOperator.Add),
                *digits("984"),
                CalcKey.Equals,
            ).isCleanCodeCandidate,
        )
        assertFalse(run(*digits("19"), CalcKey.Decimal, *digits("84")).isCleanCodeCandidate)
        assertFalse(CalcState().isCleanCodeCandidate)
    }
}
