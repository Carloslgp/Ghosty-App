package com.utils.calc.feature.calculator.engine

import java.math.BigDecimal

enum class CalcOperator(val symbol: String) {
    Add("+"),
    Subtract("−"),
    Multiply("×"),
    Divide("÷"),
}

sealed interface CalcKey {
    data class Digit(val value: Int) : CalcKey
    data object Decimal : CalcKey
    data class Operation(val operator: CalcOperator) : CalcKey
    data object Equals : CalcKey
    data object Percent : CalcKey
    data object ToggleSign : CalcKey
    data object Clear : CalcKey
    data object Backspace : CalcKey
}

enum class CalcError { DivideByZero }

data class CalculationRecord(
    val id: Long,
    val expression: String,
    val result: String,
)

data class CalcState(
    /** Texto cru do visor, com ponto decimal e sinal. Nunca formatado. */
    val entry: String = "0",
    val accumulator: BigDecimal? = null,
    val pendingOperator: CalcOperator? = null,
    val lastOperator: CalcOperator? = null,
    val lastOperand: BigDecimal? = null,
    /** Acabou de sair um operador: o próximo dígito começa uma entrada nova. */
    val awaitingOperand: Boolean = false,
    /** O visor mostra um resultado, não algo digitado. */
    val entryIsResult: Boolean = false,
    val expression: String = "",
    val error: CalcError? = null,
    val history: List<CalculationRecord> = emptyList(),
    val nextRecordId: Long = 1L,
) {
    /**
     * Um código secreto só vale quando a usuária digitou dígitos limpos: nada de
     * conta pendente, nada de resultado na tela. Assim um "1984" que caiu como
     * resultado de uma conta não abre o cofre por acidente.
     */
    val isCleanCodeCandidate: Boolean
        get() = error == null &&
            !entryIsResult &&
            !awaitingOperand &&
            pendingOperator == null &&
            accumulator == null &&
            !entry.contains('.') &&
            !entry.startsWith("-") &&
            entry != "0"

    /** Rótulo da tecla de limpeza: C limpa a entrada, AC limpa a conta inteira. */
    val clearsEntryOnly: Boolean
        get() = error == null && entry != "0" && !awaitingOperand && !entryIsResult
}
