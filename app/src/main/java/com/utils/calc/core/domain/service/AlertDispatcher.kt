package com.utils.calc.core.domain.service

import com.utils.calc.core.domain.model.AlertOutcome
import com.utils.calc.core.domain.model.AlertRequest

/**
 * Contrato do envio de alerta. A unica implementacao existente e permitida nesta
 * fase apenas registra em log e devolve o resultado para a UI — nada sai do
 * aparelho. Ver a secao de regras invioláveis no CLAUDE.md antes de mexer aqui.
 */
interface AlertDispatcher {
    suspend fun dispatch(request: AlertRequest): AlertOutcome
}
