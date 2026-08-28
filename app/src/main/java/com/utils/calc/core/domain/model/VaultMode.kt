package com.utils.calc.core.domain.model

/**
 * REAL: cofre da usuaria. DURESS: cofre plausivel aberto pelo PIN de coacao —
 * mesma interface, contatos ficticios, nenhuma gravacao. Precisa ser
 * indistinguivel do real para quem esta' olhando por cima do ombro.
 */
enum class VaultMode { REAL, DURESS }
