package com.utils.calc.feature.panic

import com.utils.calc.BuildConfig
import com.utils.calc.core.di.ApplicationScope
import com.utils.calc.core.domain.model.AlertRequest
import com.utils.calc.core.domain.model.EvidenceKind
import com.utils.calc.core.domain.model.RecordingState
import com.utils.calc.core.domain.repository.ContactsRepository
import com.utils.calc.core.domain.repository.TriggerRepository
import com.utils.calc.core.domain.service.AlertDispatcher
import com.utils.calc.core.domain.service.LocationProvider
import com.utils.calc.core.domain.service.RecordingService
import com.utils.calc.core.domain.service.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orquestra o modo de emergência. Conhece apenas interfaces de core:domain —
 * é o que permite trocar o dispatcher fake por algo real, um dia, sem tocar
 * nesta lógica.
 *
 * Roda no escopo da aplicação de propósito: o fluxo precisa sobreviver à tela
 * que o disparou.
 */
@Singleton
class PanicController @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val contactsRepository: ContactsRepository,
    private val triggerRepository: TriggerRepository,
    private val alertDispatcher: AlertDispatcher,
    private val recordingService: RecordingService,
    private val locationProvider: LocationProvider,
    private val timeSource: TimeSource,
) {

    private val _state = MutableStateFlow(PanicState())
    val state: StateFlow<PanicState> = _state.asStateFlow()

    private var runJob: Job? = null
    private var locationJob: Job? = null

    init {
        scope.launch {
            recordingService.state.collect { recording ->
                _state.update { it.copy(recording = recording) }
            }
        }
    }

    fun trigger(source: PanicSource, simulated: Boolean = false) {
        check(!BuildConfig.OUTBOUND_COMMS_ENABLED) {
            "Comunicação externa está desabilitada nesta fase do projeto."
        }
        if (_state.value.isRunning) return

        runJob?.cancel()
        runJob = scope.launch { run(source, simulated) }
    }

    /** Serve para a contagem regressiva e para a emergência já ativa: o gesto é o mesmo. */
    fun cancel() {
        val wasActive = _state.value.phase is PanicPhase.Active
        runJob?.cancel()
        runJob = null
        locationJob?.cancel()
        locationJob = null

        scope.launch {
            if (wasActive) recordingService.stop()
            _state.update {
                it.copy(
                    phase = if (wasActive) PanicPhase.Finished else PanicPhase.Cancelled,
                    endedAt = timeSource.nowMillis(),
                    log = it.log + entry(if (wasActive) PanicEvent.Finished else PanicEvent.Cancelled),
                )
            }
        }
    }

    /** Volta ao repouso sem apagar o último registro, que a tela inicial exibe. */
    fun acknowledge() {
        if (_state.value.isRunning) return
        _state.update { it.copy(phase = PanicPhase.Idle) }
    }

    private suspend fun run(source: PanicSource, simulated: Boolean) {
        val settings = triggerRepository.currentSettings()
        val startedAt = timeSource.nowMillis()

        /**
         * O PIN de coação dispara com alguém olhando: não há disfarce na tela e,
         * portanto, não há como fazer o gesto de cancelar. Esperar seria só
         * atraso. Pelo mesmo motivo o alerta é único — sem gravação aberta que
         * ela não teria como encerrar depois.
         */
        val alertOnly = source == PanicSource.DURESS_PIN
        val countdown = if (alertOnly) 0 else settings.countdownSeconds

        _state.value = PanicState(
            phase = PanicPhase.Countdown(countdown),
            source = source,
            simulated = simulated,
            disguise = settings.disguise,
            startedAt = startedAt,
            log = listOf(entry(PanicEvent.Triggered(source))),
        )

        if (countdown > 0) {
            appendLog(PanicEvent.CountdownStarted(countdown))
            for (remaining in countdown downTo 1) {
                _state.update { it.copy(phase = PanicPhase.Countdown(remaining)) }
                delay(ONE_SECOND)
            }
        }

        _state.update { it.copy(phase = PanicPhase.Dispatching) }
        appendLog(PanicEvent.DisguiseApplied(settings.disguise))

        val point = locationProvider.current()
        if (point != null) {
            _state.update { it.copy(location = point) }
            appendLog(PanicEvent.LocationCaptured(point))
        }

        // Ensaio não abre microfone nem cria evidência: seria mentir para ela
        // sobre o que ficou guardado no aparelho.
        if (!simulated && !alertOnly) {
            recordingService.start(EvidenceKind.AUDIO)
            appendLog(PanicEvent.RecordingStarted)
        }

        dispatchAlerts(simulated, point)

        if (simulated || alertOnly) {
            _state.update {
                it.copy(
                    phase = PanicPhase.Finished,
                    endedAt = timeSource.nowMillis(),
                    log = it.log + entry(PanicEvent.Finished),
                )
            }
            return
        }

        _state.update { it.copy(phase = PanicPhase.Active) }
        locationJob = scope.launch {
            locationProvider.stream().collect { updated ->
                _state.update { it.copy(location = updated) }
            }
        }
    }

    private suspend fun dispatchAlerts(
        simulated: Boolean,
        point: com.utils.calc.core.domain.model.GeoPoint?,
    ) {
        val contacts = contactsRepository.observeContacts().first()
        if (contacts.isEmpty()) {
            appendLog(PanicEvent.NoContacts)
            return
        }

        val message = contactsRepository.observeAlertMessage().first()
        val triggeredAt = timeSource.nowMillis()

        // O contato principal primeiro: se algo travar, é quem mais importa.
        contacts.sortedByDescending { it.isPrimary }.forEach { contact ->
            val outcome = alertDispatcher.dispatch(
                AlertRequest(
                    contact = contact,
                    message = message,
                    location = point,
                    triggeredAt = triggeredAt,
                    simulated = simulated,
                ),
            )
            _state.update {
                it.copy(
                    outcomes = it.outcomes + outcome,
                    log = it.log + entry(PanicEvent.AlertDispatched(outcome)),
                )
            }
        }
    }

    private fun appendLog(event: PanicEvent) {
        _state.update { it.copy(log = it.log + entry(event)) }
    }

    private fun entry(event: PanicEvent) = PanicLogEntry(timeSource.nowMillis(), event)

    private companion object {
        const val ONE_SECOND = 1_000L
    }
}

internal fun RecordingState.elapsedOrZero(): Long =
    (this as? RecordingState.Recording)?.elapsedMillis ?: 0L
