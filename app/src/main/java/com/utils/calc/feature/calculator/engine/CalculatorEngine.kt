package com.utils.calc.feature.calculator.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Motor puro da calculadora. Sem Android, sem corrotina, sem estado mutável —
 * cada tecla é uma transformação de [CalcState]. É o que permite testar a
 * fachada de verdade: uma calculadora que erra denuncia o app.
 */
object CalculatorEngine {

    const val MAX_DIGITS = 12
    const val MAX_HISTORY = 40

    private val Context = MathContext(20, RoundingMode.HALF_UP)
    private val Hundred = BigDecimal("100")

    fun press(state: CalcState, key: CalcKey): CalcState = when (key) {
        is CalcKey.Digit -> digit(state, key.value)
        CalcKey.Decimal -> decimal(state)
        is CalcKey.Operation -> operator(state, key.operator)
        CalcKey.Equals -> equals(state)
        CalcKey.Percent -> percent(state)
        CalcKey.ToggleSign -> toggleSign(state)
        CalcKey.Clear -> clear(state)
        CalcKey.Backspace -> backspace(state)
    }

    /** O texto do visor. Entrada digitada e resultado são formatados de formas diferentes. */
    fun displayText(state: CalcState): String = when {
        state.entryIsResult || state.awaitingOperand ->
            CalculatorFormatter.formatResult(state.entryValue())

        else -> CalculatorFormatter.formatEntry(state.entry)
    }

    private fun digit(state: CalcState, value: Int): CalcState {
        val base = state.recovered()
        if (base.entryIsResult || base.awaitingOperand) {
            return base.startingFresh().copy(entry = value.toString())
        }
        if (base.entry.count { it.isDigit() } >= MAX_DIGITS) return base
        val entry = when (base.entry) {
            "0" -> value.toString()
            "-0" -> "-" + value
            else -> base.entry + value
        }
        return base.copy(entry = entry)
    }

    private fun decimal(state: CalcState): CalcState {
        val base = state.recovered()
        if (base.entryIsResult || base.awaitingOperand) {
            return base.startingFresh().copy(entry = "0.")
        }
        if (base.entry.contains('.')) return base
        return base.copy(entry = base.entry + ".")
    }

    private fun toggleSign(state: CalcState): CalcState {
        val base = state.recovered()
        // Depois de um operador o visor mostra o acumulador; inverter o sinal ali
        // significa "vou digitar um número negativo agora".
        if (base.awaitingOperand) {
            return base.copy(entry = "-0", awaitingOperand = false, entryIsResult = false)
        }
        val entry = when {
            base.entry.startsWith("-") -> base.entry.substring(1)
            base.entry == "0" -> return base
            else -> "-" + base.entry
        }
        return base.copy(entry = entry)
    }

    private fun operator(state: CalcState, operator: CalcOperator): CalcState {
        val base = state.recovered()

        // Trocar de ideia sobre o operador não deve calcular nada.
        if (base.awaitingOperand && base.pendingOperator != null) {
            val accumulator = base.accumulator ?: base.entryValue()
            return base.copy(
                pendingOperator = operator,
                expression = expressionOf(accumulator, operator),
            )
        }

        val value = base.entryValue()
        val pending = base.pendingOperator
        val previous = base.accumulator
        val accumulator = if (pending != null && previous != null) {
            apply(previous, pending, value) ?: return base.failed()
        } else {
            value
        }

        return base.copy(
            entry = accumulator.plain(),
            accumulator = accumulator,
            pendingOperator = operator,
            awaitingOperand = true,
            entryIsResult = false,
            lastOperator = null,
            lastOperand = null,
            expression = expressionOf(accumulator, operator),
        )
    }

    private fun equals(state: CalcState): CalcState {
        val base = state.recovered()
        val pending = base.pendingOperator
        val accumulator = base.accumulator

        val left: BigDecimal
        val operator: CalcOperator
        val right: BigDecimal
        when {
            pending != null && accumulator != null -> {
                left = accumulator
                operator = pending
                right = base.entryValue()
            }

            // "=" repetido repete a última operação, como qualquer calculadora.
            base.lastOperator != null && base.lastOperand != null -> {
                left = base.entryValue()
                operator = base.lastOperator
                right = base.lastOperand
            }

            // "=" sobre um número solto não calcula nada, mas fecha a entrada:
            // o visor passa a ser um resultado e o próximo dígito recomeça.
            else -> return base.copy(entryIsResult = true, awaitingOperand = false)
        }

        val result = apply(left, operator, right) ?: return base.failed()
        val expression = expressionOf(left, operator) + " " +
            CalculatorFormatter.formatResult(right) + " ="
        val record = CalculationRecord(
            id = base.nextRecordId,
            expression = expression,
            result = CalculatorFormatter.formatResult(result),
        )

        return base.copy(
            entry = result.plain(),
            accumulator = null,
            pendingOperator = null,
            lastOperator = operator,
            lastOperand = right,
            awaitingOperand = false,
            entryIsResult = true,
            expression = expression,
            history = (listOf(record) + base.history).take(MAX_HISTORY),
            nextRecordId = base.nextRecordId + 1,
        )
    }

    /**
     * Em soma e subtração a porcentagem é relativa ao primeiro operando
     * (50 + 10 % = 55). Nos demais casos é só dividir por cem.
     */
    private fun percent(state: CalcState): CalcState {
        val base = state.recovered()
        val value = base.entryValue()
        val accumulator = base.accumulator
        val relative = accumulator != null &&
            (base.pendingOperator == CalcOperator.Add || base.pendingOperator == CalcOperator.Subtract)

        val result = if (relative && accumulator != null) {
            accumulator.multiply(value).divide(Hundred, Context)
        } else {
            value.divide(Hundred, Context)
        }

        return base.copy(
            entry = result.plain(),
            awaitingOperand = false,
            entryIsResult = true,
        )
    }

    private fun clear(state: CalcState): CalcState {
        if (state.error != null) return state.resetKeepingHistory()
        val hasTypedEntry = state.entry != "0" && !state.awaitingOperand && !state.entryIsResult
        return if (hasTypedEntry) state.copy(entry = "0") else state.resetKeepingHistory()
    }

    private fun backspace(state: CalcState): CalcState {
        if (state.error != null) return state.resetKeepingHistory()
        if (state.entryIsResult || state.awaitingOperand) {
            return state.copy(entry = "0", entryIsResult = false, awaitingOperand = false)
        }
        val trimmed = state.entry.dropLast(1)
        val entry = if (trimmed.isEmpty() || trimmed == "-") "0" else trimmed
        return state.copy(entry = entry)
    }

    private fun apply(
        left: BigDecimal,
        operator: CalcOperator,
        right: BigDecimal,
    ): BigDecimal? = when (operator) {
        CalcOperator.Add -> left.add(right, Context)
        CalcOperator.Subtract -> left.subtract(right, Context)
        CalcOperator.Multiply -> left.multiply(right, Context)
        CalcOperator.Divide ->
            if (right.compareTo(BigDecimal.ZERO) == 0) null else left.divide(right, Context)
    }

    private fun expressionOf(value: BigDecimal, operator: CalcOperator): String =
        CalculatorFormatter.formatResult(value) + " " + operator.symbol

    private fun CalcState.failed() = CalcState(
        error = CalcError.DivideByZero,
        history = history,
        nextRecordId = nextRecordId,
    )

    /** Qualquer tecla depois de um erro recomeça a conta, preservando o histórico. */
    private fun CalcState.recovered(): CalcState =
        if (error == null) this else resetKeepingHistory()

    private fun CalcState.resetKeepingHistory() = CalcState(
        history = history,
        nextRecordId = nextRecordId,
    )

    private fun CalcState.startingFresh(): CalcState = copy(
        entryIsResult = false,
        awaitingOperand = false,
        lastOperator = if (entryIsResult) null else lastOperator,
        lastOperand = if (entryIsResult) null else lastOperand,
        expression = if (entryIsResult) "" else expression,
    )

    private fun BigDecimal.plain(): String = stripTrailingZeros().toPlainString()
}

internal fun CalcState.entryValue(): BigDecimal {
    val normalized = entry
        .removeSuffix(".")
        .let { if (it.isEmpty() || it == "-") "0" else it }
    return runCatching { BigDecimal(normalized) }.getOrDefault(BigDecimal.ZERO)
}
