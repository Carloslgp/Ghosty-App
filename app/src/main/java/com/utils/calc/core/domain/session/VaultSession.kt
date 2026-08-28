package com.utils.calc.core.domain.session

import com.utils.calc.core.domain.model.VaultMode
import kotlinx.coroutines.flow.StateFlow

/**
 * Qual cofre esta' aberto no momento. Enquanto nenhum esta' aberto o modo
 * efetivo e' REAL: o fluxo de panico dispara com os dados verdadeiros mesmo com
 * a interface fechada.
 */
interface VaultSession {
    val mode: StateFlow<VaultMode?>

    val activeMode: VaultMode

    fun open(mode: VaultMode)

    fun close()
}
