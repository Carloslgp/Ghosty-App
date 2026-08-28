package com.utils.calc.feature.panic

import com.utils.calc.core.domain.model.AlertOutcome
import com.utils.calc.core.domain.model.GeoPoint
import com.utils.calc.core.domain.model.PanicDisguise
import com.utils.calc.core.domain.model.RecordingState

enum class PanicSource {
    MANUAL,
    CALCULATOR_CODE,
    LONG_PRESS,
    DURESS_PIN,
    SAFE_TEST,
}

sealed interface PanicPhase {
    data object Idle : PanicPhase
    data class Countdown(val secondsLeft: Int) : PanicPhase
    data object Dispatching : PanicPhase
    data object Active : PanicPhase
    data object Cancelled : PanicPhase
    data object Finished : PanicPhase
}

/**
 * O que aconteceu, em ordem. É isto que a tela de ensaio mostra e o que
 * substitui, nesta fase, qualquer envio de verdade.
 */
sealed interface PanicEvent {
    data class Triggered(val source: PanicSource) : PanicEvent
    data class CountdownStarted(val seconds: Int) : PanicEvent
    data class DisguiseApplied(val disguise: PanicDisguise) : PanicEvent
    data object RecordingStarted : PanicEvent
    data class LocationCaptured(val point: GeoPoint) : PanicEvent
    data class AlertDispatched(val outcome: AlertOutcome) : PanicEvent
    data object NoContacts : PanicEvent
    data object Cancelled : PanicEvent
    data object Finished : PanicEvent
}

data class PanicLogEntry(
    val at: Long,
    val event: PanicEvent,
)

data class PanicState(
    val phase: PanicPhase = PanicPhase.Idle,
    val source: PanicSource? = null,
    val simulated: Boolean = false,
    val disguise: PanicDisguise = PanicDisguise.CALCULATOR,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val location: GeoPoint? = null,
    val recording: RecordingState = RecordingState.Idle,
    val outcomes: List<AlertOutcome> = emptyList(),
    val log: List<PanicLogEntry> = emptyList(),
) {
    val isRunning: Boolean
        get() = phase is PanicPhase.Countdown ||
            phase is PanicPhase.Dispatching ||
            phase is PanicPhase.Active

    /**
     * O ensaio e o disparo pelo código de coação rodam sem tomar a tela: no
     * primeiro caso ela está olhando o roteiro, no segundo ela está abrindo o
     * cofre falso na frente de alguém.
     */
    val showsOverlay: Boolean
        get() = isRunning && source != PanicSource.SAFE_TEST && source != PanicSource.DURESS_PIN
}
