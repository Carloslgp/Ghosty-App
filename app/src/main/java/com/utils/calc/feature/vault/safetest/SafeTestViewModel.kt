package com.utils.calc.feature.vault.safetest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utils.calc.core.di.SessionAware
import com.utils.calc.core.domain.model.PanicDisguise
import com.utils.calc.core.domain.repository.ContactsRepository
import com.utils.calc.core.domain.repository.TriggerRepository
import com.utils.calc.core.domain.session.VaultSession
import com.utils.calc.feature.panic.PanicController
import com.utils.calc.feature.panic.PanicLogEntry
import com.utils.calc.feature.panic.PanicSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SafeTestUiState(
    val running: Boolean = false,
    val finished: Boolean = false,
    val log: List<PanicLogEntry> = emptyList(),
    val hasContacts: Boolean = false,
    val disguise: PanicDisguise = PanicDisguise.CALCULATOR,
)

@HiltViewModel
class SafeTestViewModel @Inject constructor(
    @SessionAware contactsRepository: ContactsRepository,
    @SessionAware triggerRepository: TriggerRepository,
    private val panicController: PanicController,
    private val vaultSession: VaultSession,
) : ViewModel() {

    val uiState: StateFlow<SafeTestUiState> = combine(
        panicController.state,
        contactsRepository.observeContacts(),
        triggerRepository.observeSettings(),
    ) { panic, contacts, settings ->
        val isRehearsal = panic.source == PanicSource.SAFE_TEST
        SafeTestUiState(
            running = isRehearsal && panic.isRunning,
            finished = isRehearsal && !panic.isRunning && panic.log.isNotEmpty(),
            log = if (isRehearsal) panic.log else emptyList(),
            hasContacts = contacts.isNotEmpty(),
            disguise = settings.disguise,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SafeTestUiState(),
    )

    fun onRun() = panicController.trigger(PanicSource.SAFE_TEST, simulated = true)

    fun onCloseSession() = vaultSession.close()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
