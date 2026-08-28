package com.utils.calc.core.data.repository

import com.utils.calc.core.data.fake.DuressDataStore
import com.utils.calc.core.domain.model.PanicDisguise
import com.utils.calc.core.domain.model.TriggerSettings
import com.utils.calc.core.domain.model.TriggerType
import com.utils.calc.core.domain.model.VaultMode
import com.utils.calc.core.domain.repository.TriggerRepository
import com.utils.calc.core.domain.session.VaultSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SessionAwareTriggerRepository @Inject constructor(
    private val real: TriggerRepositoryImpl,
    private val duress: DuressDataStore,
    private val session: VaultSession,
) : TriggerRepository {

    override fun observeSettings(): Flow<TriggerSettings> =
        session.mode.flatMapLatest { mode ->
            if (mode == VaultMode.DURESS) duress.triggerSettings else real.observeSettings()
        }

    override suspend fun currentSettings(): TriggerSettings =
        if (isDuress) duress.triggerSettings.value else real.currentSettings()

    override suspend fun setEnabled(type: TriggerType, enabled: Boolean) {
        if (isDuress) {
            writeDuress { it.mapTrigger(type) { c -> c.copy(enabled = enabled && c.available) } }
        } else {
            real.setEnabled(type, enabled)
        }
    }

    override suspend fun setSensitivity(type: TriggerType, sensitivity: Int) {
        if (isDuress) {
            writeDuress { it.mapTrigger(type) { c -> c.copy(sensitivity = sensitivity.coerceIn(0, 100)) } }
        } else {
            real.setSensitivity(type, sensitivity)
        }
    }

    override suspend fun setPanicCode(code: String) {
        if (isDuress) writeDuress { it.copy(panicCode = code) } else real.setPanicCode(code)
    }

    override suspend fun setCountdownSeconds(seconds: Int) {
        if (isDuress) {
            writeDuress { it.copy(countdownSeconds = seconds.coerceIn(0, 30)) }
        } else {
            real.setCountdownSeconds(seconds)
        }
    }

    override suspend fun setDisguise(disguise: PanicDisguise) {
        if (isDuress) writeDuress { it.copy(disguise = disguise) } else real.setDisguise(disguise)
    }

    override suspend fun setDuressPinAlsoAlerts(enabled: Boolean) {
        if (isDuress) {
            writeDuress { it.copy(duressPinAlsoAlerts = enabled) }
        } else {
            real.setDuressPinAlsoAlerts(enabled)
        }
    }

    private val isDuress: Boolean
        get() = session.activeMode == VaultMode.DURESS

    private fun writeDuress(transform: (TriggerSettings) -> TriggerSettings) {
        duress.writeTriggerSettings(transform(duress.triggerSettings.value))
    }
}
