package com.utils.calc.core.domain.model

/**
 * Marcadores que a mensagem de alerta aceita. Vivem no dominio porque a tela de
 * contatos precisa mostrar a previa com eles, e ela nao pode enxergar a
 * implementacao do dispatcher.
 */
object AlertMessageTemplate {
    const val PLACEHOLDER_NAME = "{nome}"
    const val PLACEHOLDER_PLACE = "{local}"
}

data class AlertRequest(
    val contact: TrustedContact,
    val message: String,
    val location: GeoPoint?,
    val triggeredAt: Long,
    val simulated: Boolean,
)

enum class AlertStatus {
    /** Unico status possivel nesta fase: o alerta foi montado, registrado e descartado. */
    SIMULATED,

    /** A montagem falhou (contato invalido, mensagem vazia). */
    FAILED,
}

data class AlertOutcome(
    val contactId: String,
    val contactName: String,
    val status: AlertStatus,
    val renderedMessage: String,
    val at: Long,
    val detail: String,
)
