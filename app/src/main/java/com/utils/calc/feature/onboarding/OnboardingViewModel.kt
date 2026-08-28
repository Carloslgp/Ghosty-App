package com.utils.calc.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utils.calc.core.domain.model.PinKind
import com.utils.calc.core.domain.model.PinRules
import com.utils.calc.core.domain.model.TrustedContact
import com.utils.calc.core.domain.model.VaultMode
import com.utils.calc.core.domain.repository.ContactsRepository
import com.utils.calc.core.domain.repository.SecurityRepository
import com.utils.calc.core.domain.session.VaultSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class OnboardingStep { AccessPin, DuressPin, Contacts, Permissions }

enum class PinProblem { TooShort, TooObvious, Mismatch, SameAsAccess }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.AccessPin,
    val pin: String = "",
    val confirming: Boolean = false,
    val problem: PinProblem? = null,
    val contacts: List<TrustedContact> = emptyList(),
    val saving: Boolean = false,
) {
    val stepNumber: Int get() = step.ordinal + 1
    val canAdvancePin: Boolean get() = pin.length >= PinRules.MIN_LENGTH
    val canAddContact: Boolean get() = contacts.size < ContactsRepository.MAX_CONTACTS
}

sealed interface OnboardingEvent {
    data object Completed : OnboardingEvent
    data object Abandoned : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val contactsRepository: ContactsRepository,
    private val vaultSession: VaultSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val uiEvents = events.receiveAsFlow()

    private var accessPin: String? = null
    private var firstEntry: String? = null

    fun onDigit(digit: Int) {
        _uiState.update { state ->
            if (state.pin.length >= PinRules.MAX_LENGTH) {
                state
            } else {
                state.copy(pin = state.pin + digit, problem = null)
            }
        }
    }

    fun onBackspace() {
        _uiState.update { it.copy(pin = it.pin.dropLast(1), problem = null) }
    }

    fun onPinContinue() {
        val state = _uiState.value
        val pin = state.pin

        if (!PinRules.isWellFormed(pin)) {
            _uiState.update { it.copy(problem = PinProblem.TooShort) }
            return
        }
        if (PinRules.isTooObvious(pin)) {
            _uiState.update { it.copy(problem = PinProblem.TooObvious) }
            return
        }

        if (!state.confirming) {
            firstEntry = pin
            _uiState.update { it.copy(pin = "", confirming = true, problem = null) }
            return
        }

        if (pin != firstEntry) {
            _uiState.update { it.copy(pin = "", confirming = false, problem = PinProblem.Mismatch) }
            firstEntry = null
            return
        }

        when (state.step) {
            OnboardingStep.AccessPin -> {
                accessPin = pin
                viewModelScope.launch { securityRepository.setPin(PinKind.ACCESS, pin) }
                moveTo(OnboardingStep.DuressPin)
            }

            OnboardingStep.DuressPin -> {
                if (pin == accessPin) {
                    _uiState.update {
                        it.copy(pin = "", confirming = false, problem = PinProblem.SameAsAccess)
                    }
                    firstEntry = null
                    return
                }
                viewModelScope.launch { securityRepository.setPin(PinKind.DURESS, pin) }
                moveTo(OnboardingStep.Contacts)
            }

            else -> Unit
        }
    }

    fun onAddContact(name: String, phone: String, relationship: String) {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        if (trimmedName.isEmpty() || trimmedPhone.isEmpty()) return

        val state = _uiState.value
        if (!state.canAddContact) return

        val contact = TrustedContact(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            phone = trimmedPhone,
            relationship = relationship.trim(),
            isPrimary = state.contacts.isEmpty(),
        )
        _uiState.update { it.copy(contacts = it.contacts + contact) }
    }

    fun onRemoveContact(id: String) {
        _uiState.update { current ->
            val remaining = current.contacts.filterNot { it.id == id }
            val normalized = if (remaining.none { it.isPrimary }) {
                remaining.mapIndexed { index, contact -> contact.copy(isPrimary = index == 0) }
            } else {
                remaining
            }
            current.copy(contacts = normalized)
        }
    }

    fun onContactsContinue() = moveTo(OnboardingStep.Permissions)

    fun onBack() {
        val state = _uiState.value
        if (state.confirming) {
            firstEntry = null
            _uiState.update { it.copy(pin = "", confirming = false, problem = null) }
            return
        }
        when (state.step) {
            OnboardingStep.AccessPin -> viewModelScope.launch {
                events.send(OnboardingEvent.Abandoned)
            }

            OnboardingStep.DuressPin -> moveTo(OnboardingStep.AccessPin)
            OnboardingStep.Contacts -> moveTo(OnboardingStep.DuressPin)
            OnboardingStep.Permissions -> moveTo(OnboardingStep.Contacts)
        }
    }

    fun onFinish() {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            _uiState.value.contacts.forEach { contactsRepository.upsert(it) }
            securityRepository.completeOnboarding()
            vaultSession.open(VaultMode.REAL)
            events.send(OnboardingEvent.Completed)
        }
    }

    private fun moveTo(step: OnboardingStep) {
        firstEntry = null
        _uiState.update {
            it.copy(step = step, pin = "", confirming = false, problem = null)
        }
    }
}
