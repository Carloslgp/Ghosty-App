package com.utils.calc.feature.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utils.calc.core.domain.model.CodeMatch
import com.utils.calc.core.domain.model.PinKind
import com.utils.calc.core.domain.model.TriggerSettings
import com.utils.calc.core.domain.model.TriggerType
import com.utils.calc.core.domain.model.VaultMode
import com.utils.calc.core.domain.repository.SecurityRepository
import com.utils.calc.core.domain.repository.TriggerRepository
import com.utils.calc.core.domain.session.VaultSession
import com.utils.calc.feature.calculator.engine.CalcKey
import com.utils.calc.feature.calculator.engine.CalculationRecord
import com.utils.calc.feature.calculator.engine.CalcState
import com.utils.calc.feature.calculator.engine.CalculatorEngine
import com.utils.calc.feature.panic.PanicController
import com.utils.calc.feature.panic.PanicSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CalculatorEvent {
    data object OpenOnboarding : CalculatorEvent
    data object OpenVault : CalculatorEvent
}

data class CalculatorUiState(
    val display: String = "0",
    val expression: String = "",
    val hasError: Boolean = false,
    val clearsEntryOnly: Boolean = false,
    val history: List<CalculationRecord> = emptyList(),
    val showSetupHint: Boolean = false,
    val setupCode: String = TriggerSettings.SETUP_CODE,
    val longPressTriggerArmed: Boolean = false,
)

/**
 * A calculadora é a única tela que o app mostra por padrão, e ela funciona de
 * verdade. Os gatilhos vivem aqui como um desvio raro: só quando a usuária
 * digitou dígitos limpos e tocou em "=".
 */
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val triggerRepository: TriggerRepository,
    private val vaultSession: VaultSession,
    private val panicController: PanicController,
) : ViewModel() {

    private val calcState = MutableStateFlow(CalcState())
    private val setupPending = securityRepository.observeOnboardingCompleted()
    private val triggerSettings = triggerRepository.observeSettings()

    private val events = Channel<CalculatorEvent>(Channel.BUFFERED)
    val uiEvents = events.receiveAsFlow()

    val uiState: StateFlow<CalculatorUiState> =
        combine(calcState, setupPending, triggerSettings) { state, completed, settings ->
            CalculatorUiState(
                display = CalculatorEngine.displayText(state),
                expression = state.expression,
                hasError = state.error != null,
                clearsEntryOnly = state.clearsEntryOnly,
                history = state.history,
                showSetupHint = !completed,
                longPressTriggerArmed = completed && settings.isEnabled(TriggerType.KEY_LONG_PRESS),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CalculatorUiState(),
        )

    fun onKey(key: CalcKey) {
        if (key is CalcKey.Equals) {
            onEquals()
            return
        }
        calcState.value = CalculatorEngine.press(calcState.value, key)
    }

    fun onClearHistory() {
        calcState.value = calcState.value.copy(history = emptyList())
    }

    /**
     * Toque longo de 3 s no zero. Antes da configuração ele abre o onboarding —
     * é a porta de entrada de quem não conhece o código ainda.
     */
    fun onZeroLongPress() {
        viewModelScope.launch {
            if (!securityRepository.isOnboardingCompleted()) {
                events.send(CalculatorEvent.OpenOnboarding)
                return@launch
            }
            if (triggerRepository.currentSettings().isEnabled(TriggerType.KEY_LONG_PRESS)) {
                panicController.trigger(PanicSource.LONG_PRESS)
            }
        }
    }

    private fun onEquals() {
        val snapshot = calcState.value
        if (!snapshot.isCleanCodeCandidate) {
            calcState.value = CalculatorEngine.press(snapshot, CalcKey.Equals)
            return
        }

        val typed = snapshot.entry
        viewModelScope.launch {
            if (!securityRepository.isOnboardingCompleted()) {
                if (typed == TriggerSettings.SETUP_CODE) {
                    clearTrace()
                    events.send(CalculatorEvent.OpenOnboarding)
                } else {
                    calcState.value = CalculatorEngine.press(calcState.value, CalcKey.Equals)
                }
                return@launch
            }

            when (val match = securityRepository.match(typed)) {
                is CodeMatch.Pin -> openVault(match.kind)
                CodeMatch.PanicCode -> {
                    clearTrace()
                    panicController.trigger(PanicSource.CALCULATOR_CODE)
                }

                CodeMatch.None ->
                    calcState.value = CalculatorEngine.press(calcState.value, CalcKey.Equals)
            }
        }
    }

    private suspend fun openVault(kind: PinKind) {
        clearTrace()
        when (kind) {
            PinKind.ACCESS -> vaultSession.open(VaultMode.REAL)
            PinKind.DURESS -> {
                vaultSession.open(VaultMode.DURESS)
                if (triggerRepository.currentSettings().duressPinAlsoAlerts) {
                    panicController.trigger(PanicSource.DURESS_PIN)
                }
            }
        }
        events.send(CalculatorEvent.OpenVault)
    }

    /**
     * O código digitado não pode ficar no visor nem virar linha de histórico:
     * quem pegar o aparelho depois não deve encontrar rastro dele.
     */
    private fun clearTrace() {
        val current = calcState.value
        calcState.value = CalcState(
            history = current.history,
            nextRecordId = current.nextRecordId,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
