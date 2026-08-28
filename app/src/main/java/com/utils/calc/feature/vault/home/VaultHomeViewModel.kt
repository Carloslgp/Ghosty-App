package com.utils.calc.feature.vault.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utils.calc.core.di.SessionAware
import com.utils.calc.core.domain.model.TriggerType
import com.utils.calc.core.domain.model.VaultMode
import com.utils.calc.core.domain.repository.ContactsRepository
import com.utils.calc.core.domain.repository.EvidenceRepository
import com.utils.calc.core.domain.repository.SecurityRepository
import com.utils.calc.core.domain.repository.TriggerRepository
import com.utils.calc.core.domain.session.VaultSession
import com.utils.calc.feature.panic.PanicController
import com.utils.calc.feature.panic.PanicLogEntry
import com.utils.calc.feature.panic.PanicPhase
import com.utils.calc.feature.panic.PanicSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultHomeUiState(
    val mode: VaultMode = VaultMode.REAL,
    val contactCount: Int = 0,
    val activeTriggerCount: Int = 0,
    val panicCode: String? = null,
    val evidenceCount: Int = 0,
    val lastRunLog: List<PanicLogEntry> = emptyList(),
    val lastRunCancelled: Boolean = false,
    val lastRunSimulated: Boolean = false,
) {
    val isDuress: Boolean get() = mode == VaultMode.DURESS
    val hasContacts: Boolean get() = contactCount > 0
}

@HiltViewModel
class VaultHomeViewModel @Inject constructor(
    @SessionAware contactsRepository: ContactsRepository,
    @SessionAware triggerRepository: TriggerRepository,
    @SessionAware evidenceRepository: EvidenceRepository,
    private val securityRepository: SecurityRepository,
    private val vaultSession: VaultSession,
    private val panicController: PanicController,
) : ViewModel() {

    val uiState: StateFlow<VaultHomeUiState> = combine(
        contactsRepository.observeContacts(),
        triggerRepository.observeSettings(),
        evidenceRepository.observeAll(),
        panicController.state,
        vaultSession.mode,
    ) { contacts, settings, evidence, panic, mode ->
        val finished = panic.phase is PanicPhase.Finished || panic.phase is PanicPhase.Cancelled
        VaultHomeUiState(
            mode = mode ?: VaultMode.REAL,
            contactCount = contacts.size,
            activeTriggerCount = settings.triggers.count { it.enabled && it.available },
            panicCode = settings.panicCode.takeIf {
                settings.isEnabled(TriggerType.CALCULATOR_CODE) && it.isNotBlank()
            },
            evidenceCount = evidence.size,
            lastRunLog = if (finished) panic.log else emptyList(),
            lastRunCancelled = panic.phase is PanicPhase.Cancelled,
            lastRunSimulated = panic.simulated,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = VaultHomeUiState(),
    )

    fun onPanic() = panicController.trigger(PanicSource.MANUAL)

    fun onCloseSession() = vaultSession.close()

    fun onResetEverything() {
        viewModelScope.launch {
            securityRepository.reset()
            vaultSession.close()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
