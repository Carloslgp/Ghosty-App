package com.utils.calc.testing

import com.utils.calc.core.domain.model.AlertOutcome
import com.utils.calc.core.domain.model.AlertRequest
import com.utils.calc.core.domain.model.AlertStatus
import com.utils.calc.core.domain.model.CodeMatch
import com.utils.calc.core.domain.model.EvidenceItem
import com.utils.calc.core.domain.model.EvidenceKind
import com.utils.calc.core.domain.model.GeoPoint
import com.utils.calc.core.domain.model.PanicDisguise
import com.utils.calc.core.domain.model.PinKind
import com.utils.calc.core.domain.model.RecordingState
import com.utils.calc.core.domain.model.TriggerSettings
import com.utils.calc.core.domain.model.TriggerType
import com.utils.calc.core.domain.model.TrustedContact
import com.utils.calc.core.domain.model.VaultMode
import com.utils.calc.core.domain.repository.ContactsRepository
import com.utils.calc.core.domain.repository.EvidenceRepository
import com.utils.calc.core.domain.repository.SecurityRepository
import com.utils.calc.core.domain.repository.TriggerRepository
import com.utils.calc.core.domain.service.AlertDispatcher
import com.utils.calc.core.domain.service.LocationProvider
import com.utils.calc.core.domain.service.RecordingService
import com.utils.calc.core.domain.service.TimeSource
import com.utils.calc.core.domain.session.VaultSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

fun contact(id: String, name: String, primary: Boolean = false) = TrustedContact(
    id = id,
    name = name,
    phone = "(11) 90000-0000",
    relationship = "Teste",
    isPrimary = primary,
)

class FakeTimeSource(private var now: Long = 1_000L) : TimeSource {
    override fun nowMillis(): Long = now
    fun advance(millis: Long) {
        now += millis
    }
}

class FakeContactsRepository(
    contacts: List<TrustedContact> = emptyList(),
    message: String = ContactsRepository.DEFAULT_MESSAGE,
) : ContactsRepository {
    val contacts = MutableStateFlow(contacts)
    val message = MutableStateFlow(message)

    override fun observeContacts(): Flow<List<TrustedContact>> = this.contacts
    override fun observeAlertMessage(): Flow<String> = message
    override suspend fun upsert(contact: TrustedContact) {
        contacts.value = contacts.value.filterNot { it.id == contact.id } + contact
    }

    override suspend fun delete(contactId: String) {
        contacts.value = contacts.value.filterNot { it.id == contactId }
    }

    override suspend fun setPrimary(contactId: String) {
        contacts.value = contacts.value.map { it.copy(isPrimary = it.id == contactId) }
    }

    override suspend fun setAlertMessage(message: String) {
        this.message.value = message
    }
}

class FakeTriggerRepository(
    settings: TriggerSettings = TriggerSettings.Default,
) : TriggerRepository {
    val settings = MutableStateFlow(settings)

    override fun observeSettings(): Flow<TriggerSettings> = settings
    override suspend fun currentSettings(): TriggerSettings = settings.value
    override suspend fun setEnabled(type: TriggerType, enabled: Boolean) {
        settings.value = settings.value.copy(
            triggers = settings.value.triggers.map {
                if (it.type == type) it.copy(enabled = enabled) else it
            },
        )
    }

    override suspend fun setSensitivity(type: TriggerType, sensitivity: Int) = Unit
    override suspend fun setPanicCode(code: String) {
        settings.value = settings.value.copy(panicCode = code)
    }

    override suspend fun setCountdownSeconds(seconds: Int) {
        settings.value = settings.value.copy(countdownSeconds = seconds)
    }

    override suspend fun setDisguise(disguise: PanicDisguise) {
        settings.value = settings.value.copy(disguise = disguise)
    }

    override suspend fun setDuressPinAlsoAlerts(enabled: Boolean) {
        settings.value = settings.value.copy(duressPinAlsoAlerts = enabled)
    }
}

class RecordingAlertDispatcher(private val timeSource: TimeSource) : AlertDispatcher {
    val requests = mutableListOf<AlertRequest>()

    override suspend fun dispatch(request: AlertRequest): AlertOutcome {
        requests += request
        return AlertOutcome(
            contactId = request.contact.id,
            contactName = request.contact.name,
            status = AlertStatus.SIMULATED,
            renderedMessage = request.message,
            at = timeSource.nowMillis(),
            detail = "teste",
        )
    }
}

class FakeRecordingService : RecordingService {
    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val state: StateFlow<RecordingState> = _state.asStateFlow()

    var startCount = 0
    var stopCount = 0

    override suspend fun start(kind: EvidenceKind) {
        startCount++
        _state.value = RecordingState.Recording(0L, 0L, kind)
    }

    override suspend fun stop(): String? {
        stopCount++
        _state.value = RecordingState.Idle
        return "evidencia-teste"
    }
}

class FakeLocationProvider(
    private val point: GeoPoint? = GeoPoint(-23.5, -46.6, 10f, 1_000L),
) : LocationProvider {
    override fun stream(): Flow<GeoPoint> = point?.let { flowOf(it) } ?: flowOf()
    override suspend fun current(): GeoPoint? = point
}

class FakeEvidenceRepository : EvidenceRepository {
    val items = MutableStateFlow(emptyList<EvidenceItem>())
    override fun observeAll(): Flow<List<EvidenceItem>> = items
    override suspend fun add(item: EvidenceItem) {
        items.value = items.value + item
    }

    override suspend fun delete(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        items.value = emptyList()
    }
}

class FakeSecurityRepository(
    var accessPin: String? = null,
    var duressPin: String? = null,
    private val triggerRepository: TriggerRepository = FakeTriggerRepository(),
) : SecurityRepository {
    val completed = MutableStateFlow(false)

    override fun observeOnboardingCompleted(): Flow<Boolean> = completed
    override suspend fun isOnboardingCompleted(): Boolean = completed.value

    override suspend fun setPin(kind: PinKind, pin: String) {
        when (kind) {
            PinKind.ACCESS -> accessPin = pin
            PinKind.DURESS -> duressPin = pin
        }
    }

    override suspend fun hasPin(kind: PinKind): Boolean = when (kind) {
        PinKind.ACCESS -> accessPin != null
        PinKind.DURESS -> duressPin != null
    }

    override suspend fun match(code: String): CodeMatch {
        if (!completed.value) return CodeMatch.None
        if (code == accessPin) return CodeMatch.Pin(PinKind.ACCESS)
        if (code == duressPin) return CodeMatch.Pin(PinKind.DURESS)
        val settings = triggerRepository.currentSettings()
        val armed = settings.isEnabled(TriggerType.CALCULATOR_CODE)
        return if (armed && code == settings.panicCode) CodeMatch.PanicCode else CodeMatch.None
    }

    override suspend fun completeOnboarding() {
        completed.value = true
    }

    override suspend fun reset() {
        accessPin = null
        duressPin = null
        completed.value = false
    }
}

class FakeVaultSession : VaultSession {
    private val _mode = MutableStateFlow<VaultMode?>(null)
    override val mode: StateFlow<VaultMode?> = _mode.asStateFlow()
    override val activeMode: VaultMode get() = _mode.value ?: VaultMode.REAL
    override fun open(mode: VaultMode) {
        _mode.value = mode
    }

    override fun close() {
        _mode.value = null
    }
}
