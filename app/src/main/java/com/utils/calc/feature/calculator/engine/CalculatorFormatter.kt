package com.utils.calc.feature.calculator.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale

/**
 * Formatacao pt-BR feita a mao em vez de NumberFormat: o visor precisa formatar
 * texto sendo digitado ("1.234," com a virgula ainda solta), coisa que um
 * formatador de numero nao sabe fazer.
 */
object CalculatorFormatter {

    const val GROUP_SEPARATOR = '.'
    const val DECIMAL_SEPARATOR = ','
    const val MAX_SIGNIFICANT_DIGITS = 12

    private val DisplayContext = MathContext(MAX_SIGNIFICANT_DIGITS, RoundingMode.HALF_UP)
    private val ScientificUpperBound: BigDecimal = BigDecimal("1E+12")
    private val ScientificLowerBound: BigDecimal = BigDecimal("1E-9")

    /** Formata um resultado ja' calculado. */
    fun formatResult(value: BigDecimal): String {
        val rounded = value.round(DisplayContext).stripTrailingZeros()
        if (rounded.compareTo(BigDecimal.ZERO) == 0) return "0"

        val magnitude = rounded.abs()
        if (magnitude >= ScientificUpperBound || magnitude < ScientificLowerBound) {
            return scientific(rounded)
        }

        val plain = rounded.toPlainString()
        val negative = plain.startsWith("-")
        val unsigned = if (negative) plain.substring(1) else plain
        val parts = unsigned.split(".")
        val formatted = buildString {
            if (negative) append('-')
            append(group(parts[0]))
            if (parts.size > 1) {
                append(DECIMAL_SEPARATOR)
                append(parts[1])
            }
        }
        return formatted
    }

    /** Formata o texto que esta' sendo digitado, preservando virgula e zeros a' direita. */
    fun formatEntry(entry: String): String {
        val negative = entry.startsWith("-")
        val unsigned = if (negative) entry.substring(1) else entry
        val dotIndex = unsigned.indexOf('.')
        val intPart = if (dotIndex >= 0) unsigned.substring(0, dotIndex) else unsigned
        val fracPart = if (dotIndex >= 0) unsigned.substring(dotIndex + 1) else null

        return buildString {
            if (negative) append('-')
            append(group(intPart.ifEmpty { "0" }))
            if (fracPart != null) {
                append(DECIMAL_SEPARATOR)
                append(fracPart)
            }
        }
    }

    private fun group(digits: String): String {
        if (digits.length <= 3) return digits
        val builder = StringBuilder()
        val offset = digits.length % 3
        if (offset > 0) builder.append(digits, 0, offset)
        var index = offset
        while (index < digits.length) {
            if (builder.isNotEmpty()) builder.append(GROUP_SEPARATOR)
            builder.append(digits, index, index + 3)
            index += 3
        }
        return builder.toString()
    }

    private fun scientific(value: BigDecimal): String =
        String.format(Locale.US, "%.6E", value)
            .replace("E+0", "E+")
            .replace("E-0", "E-")
            .replace('.', DECIMAL_SEPARATOR)
}
