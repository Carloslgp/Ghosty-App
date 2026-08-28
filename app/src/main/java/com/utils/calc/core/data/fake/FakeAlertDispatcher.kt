package com.utils.calc.core.data.fake

import android.util.Log
import com.utils.calc.BuildConfig
import com.utils.calc.core.domain.model.AlertMessageTemplate
import com.utils.calc.core.domain.model.AlertOutcome
import com.utils.calc.core.domain.model.AlertRequest
import com.utils.calc.core.domain.model.AlertStatus
import com.utils.calc.core.domain.service.AlertDispatcher
import com.utils.calc.core.domain.service.TimeSource
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unica implementacao de [AlertDispatcher] que pode existir nesta fase.
 * Nao envia SMS, nao liga, nao abre socket. Monta a mensagem, registra em log e
 * devolve o resultado para a UI. Ver as regras invioláveis no CLAUDE.md.
 */
@Singleton
class FakeAlertDispatcher @Inject constructor(
    private val timeSource: TimeSource,
) : AlertDispatcher {

    override suspend fun dispatch(request: AlertRequest): AlertOutcome {
        check(!BuildConfig.OUTBOUND_COMMS_ENABLED) {
            "Comunicacao externa esta desabilitada nesta fase do projeto."
        }

        // Latencia artificial: sem ela a UI mostra tudo pronto no mesmo frame e
        // o ensaio nao se parece com o que aconteceria de verdade.
        delay(SIMULATED_LATENCY_MILLIS)

        val message = renderMessage(request)
        val outcome = if (message.isBlank() || request.contact.name.isBlank()) {
            AlertOutcome(
                contactId = request.contact.id,
                contactName = request.contact.name,
                status = AlertStatus.FAILED,
                renderedMessage = message,
                at = timeSource.nowMillis(),
                detail = "Contato ou mensagem incompletos.",
            )
        } else {
            AlertOutcome(
                contactId = request.contact.id,
                contactName = request.contact.name,
                status = AlertStatus.SIMULATED,
                renderedMessage = message,
                at = timeSource.nowMillis(),
                detail = if (request.simulated) "Ensaio: nada foi enviado." else "Simulado: nada saiu do aparelho.",
            )
        }

        // Sem nome, telefone, mensagem ou coordenada no log — nem em debug.
        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "dispatch id=${request.contact.id} status=${outcome.status} " +
                    "ensaio=${request.simulated} temLocal=${request.location != null}",
            )
        }
        return outcome
    }

    private fun renderMessage(request: AlertRequest): String {
        val place = request.location
            ?.let { "%.5f, %.5f".format(it.latitude, it.longitude) }
            ?: "localizacao indisponivel"
        return request.message
            .replace(AlertMessageTemplate.PLACEHOLDER_NAME, request.contact.name)
            .replace(AlertMessageTemplate.PLACEHOLDER_PLACE, place)
            .trim()
    }

    private companion object {
        const val TAG = "Dispatch"
        const val SIMULATED_LATENCY_MILLIS = 450L
    }
}
