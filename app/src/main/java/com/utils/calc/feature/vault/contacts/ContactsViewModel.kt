package com.utils.calc.feature.vault.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utils.calc.core.di.SessionAware
import com.utils.calc.core.domain.model.AlertMessageTemplate
import com.utils.calc.core.domain.model.TrustedContact
import com.utils.calc.core.domain.repository.ContactsRepository
import com.utils.calc.core.domain.session.VaultSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ContactsUiState(
    val contacts: List<TrustedContact> = emptyList(),
    val alertMessage: String = "",
) {
    val canAdd: Boolean get() = contacts.size < ContactsRepository.MAX_CONTACTS

    /** Prévia com um contato de exemplo: mostra como os marcadores são trocados. */
    fun previewMessage(): String {
        val name = contacts.firstOrNull { it.isPrimary }?.name
            ?: contacts.firstOrNull()?.name
            ?: "Ana"
        return alertMessage
            .replace(AlertMessageTemplate.PLACEHOLDER_NAME, name)
            .replace(AlertMessageTemplate.PLACEHOLDER_PLACE, "-23,55052, -46,63331")
    }
}

@HiltViewModel
class ContactsViewModel @Inject constructor(
    @SessionAware private val contactsRepository: ContactsRepository,
    private val vaultSession: VaultSession,
) : ViewModel() {

    val uiState: StateFlow<ContactsUiState> = combine(
        contactsRepository.observeContacts(),
        contactsRepository.observeAlertMessage(),
    ) { contacts, message ->
        ContactsUiState(contacts = contacts, alertMessage = message)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ContactsUiState(),
    )

    fun onSave(id: String?, name: String, phone: String, relationship: String) {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        if (trimmedName.isEmpty() || trimmedPhone.isEmpty()) return

        viewModelScope.launch {
            val existing = uiState.value.contacts.firstOrNull { it.id == id }
            contactsRepository.upsert(
                TrustedContact(
                    id = id ?: UUID.randomUUID().toString(),
                    name = trimmedName,
                    phone = trimmedPhone,
                    relationship = relationship.trim(),
                    isPrimary = existing?.isPrimary ?: uiState.value.contacts.isEmpty(),
                ),
            )
        }
    }

    fun onDelete(id: String) {
        viewModelScope.launch { contactsRepository.delete(id) }
    }

    fun onSetPrimary(id: String) {
        viewModelScope.launch { contactsRepository.setPrimary(id) }
    }

    fun onMessageChanged(message: String) {
        viewModelScope.launch { contactsRepository.setAlertMessage(message) }
    }

    fun onCloseSession() = vaultSession.close()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
