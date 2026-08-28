package com.utils.calc.feature.vault.triggers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utils.calc.core.di.SessionAware
import com.utils.calc.core.domain.model.CodeMatch
import com.utils.calc.core.domain.model.PanicDisguise
import com.utils.calc.core.domain.model.TriggerSettings
import com.utils.calc.core.domain.model.TriggerType
import com.utils.calc.core.domain.repository.SecurityRepository
import com.utils.calc.core.domain.repository.TriggerRepository
import com.utils.calc.core.domain.session.VaultSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PanicCodeProblem { TooShort, Conflicts }

data class TriggersUiState(
    val settings: TriggerSettings = TriggerSettings.Default,
    val codeProblem: PanicCodeProblem? = null,
)

@HiltViewModel
class TriggersViewModel @Inject constructor(
    @SessionAware private val triggerRepository: TriggerRepository,
    private val securityRepository: SecurityRepository,
    private val vaultSession: VaultSession,
) : ViewModel() {

    private val codeProblem = MutableStateFlow<PanicCodeProblem?>(null)

    val uiState: StateFlow<TriggersUiState> = combine(
        triggerRepository.observeSettings(),
        codeProblem,
    ) { settings, problem ->
        TriggersUiState(settings = settings, codeProblem = problem)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TriggersUiState(),
    )

    fun onToggle(type: TriggerType, enabled: Boolean) {
        viewModelScope.launch { triggerRepository.setEnabled(type, enabled) }
    }

    fun onSensitivity(type: TriggerType, value: Int) {
        viewModelScope.launch { triggerRepository.setSensitivity(type, value) }
    }

    /**
     * Um código de emergência igual a um dos PINs nunca dispararia: o PIN vence
     * a comparação e abriria o cofre. Melhor recusar aqui do que deixar ela
     * confiar num gatilho morto.
     */
    fun onPanicCodeChanged(code: String) {
        val digits = code.filter { it.isDigit() }.take(MAX_CODE_LENGTH)
        viewModelScope.launch {
            if (digits.length < MIN_CODE_LENGTH) {
                codeProblem.value = PanicCodeProblem.TooShort
                triggerRepository.setPanicCode(digits)
                return@launch
            }
            if (securityRepository.match(digits) is CodeMatch.Pin) {
                codeProblem.value = PanicCodeProblem.Conflicts
                return@launch
            }
            codeProblem.value = null
            triggerRepository.setPanicCode(digits)
        }
    }

    fun onCountdownChanged(seconds: Int) {
        viewModelScope.launch { triggerRepository.setCountdownSeconds(seconds) }
    }

    fun onDisguiseChanged(disguise: PanicDisguise) {
        viewModelScope.launch { triggerRepository.setDisguise(disguise) }
    }

    fun onDuressAlertChanged(enabled: Boolean) {
        viewModelScope.launch { triggerRepository.setDuressPinAlsoAlerts(enabled) }
    }

    fun onCloseSession() = vaultSession.close()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MIN_CODE_LENGTH = 3
        const val MAX_CODE_LENGTH = 8
    }
}
