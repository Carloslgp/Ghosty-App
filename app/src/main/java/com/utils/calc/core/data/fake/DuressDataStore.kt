package com.utils.calc.core.data.fake

import com.utils.calc.core.domain.model.EvidenceItem
import com.utils.calc.core.domain.model.TriggerSettings
import com.utils.calc.core.domain.model.TrustedContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Espelho volatil do cofre. Tudo o que a usuaria fizer aqui sob coacao se
 * comporta normalmente e desaparece quando o cofre falso e' fechado, para que a
 * proxima abertura sob coacao seja identica a' primeira.
 */
@Singleton
class DuressDataStore @Inject constructor() {

    private val _contacts = MutableStateFlow(DuressSeed.contacts)
    val contacts: StateFlow<List<TrustedContact>> = _contacts.asStateFlow()

    private val _alertMessage = MutableStateFlow(DuressSeed.ALERT_MESSAGE)
    val alertMessage: StateFlow<String> = _alertMessage.asStateFlow()

    private val _triggerSettings = MutableStateFlow(DuressSeed.triggerSettings)
    val triggerSettings: StateFlow<TriggerSettings> = _triggerSettings.asStateFlow()

    private val _evidence = MutableStateFlow(emptyList<EvidenceItem>())
    val evidence: StateFlow<List<EvidenceItem>> = _evidence.asStateFlow()

    fun writeContacts(list: List<TrustedContact>) {
        _contacts.value = list
    }

    fun writeAlertMessage(message: String) {
        _alertMessage.value = message
    }

    fun writeTriggerSettings(settings: TriggerSettings) {
        _triggerSettings.value = settings
    }

    fun writeEvidence(items: List<EvidenceItem>) {
        _evidence.value = items
    }

    fun reseed() {
        _contacts.value = DuressSeed.contacts
        _alertMessage.value = DuressSeed.ALERT_MESSAGE
        _triggerSettings.value = DuressSeed.triggerSettings
        _evidence.value = emptyList()
    }
}
