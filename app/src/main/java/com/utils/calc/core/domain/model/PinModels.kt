package com.utils.calc.core.domain.model

enum class PinKind { ACCESS, DURESS }

sealed interface CodeMatch {
    data class Pin(val kind: PinKind) : CodeMatch
    data object PanicCode : CodeMatch
    data object None : CodeMatch
}

object PinRules {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8

    fun isWellFormed(pin: String): Boolean =
        pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it.isDigit() }

    /** Sequencias obvias (0000, 1234, 4321) sao as primeiras que um agressor tenta. */
    fun isTooObvious(pin: String): Boolean {
        if (pin.length < 2) return true
        if (pin.all { it == pin[0] }) return true
        val ascending = pin.zipWithNext().all { (a, b) -> b - a == 1 }
        val descending = pin.zipWithNext().all { (a, b) -> a - b == 1 }
        return ascending || descending
    }
}
