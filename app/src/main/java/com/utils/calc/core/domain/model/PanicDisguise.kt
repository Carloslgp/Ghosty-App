package com.utils.calc.core.domain.model

/** Aparencia que a tela assume enquanto o modo de emergencia esta' ativo. */
enum class PanicDisguise {
    /** Calculadora aparentemente normal, respondendo aos toques. */
    CALCULATOR,

    /** Tela preta, como se o aparelho estivesse desligado. */
    BLACK_SCREEN,

    /** Aviso de bateria fraca, justificando a tela travada. */
    LOW_BATTERY,
}
