package com.utils.calc.core.data.repository

import com.utils.calc.core.data.local.TriggersLocalStore
import com.utils.calc.core.domain.model.PanicDisguise
import com.utils.calc.core.domain.model.TriggerConfig
import com.utils.calc.core.domain.model.TriggerSettings
import com.utils.calc.core.domain.model.TriggerType
import com.utils.calc.core.domain.repository.TriggerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerRepositoryImpl @Inject constructor(
    private val store: TriggersLocalStore,
) : TriggerRepository {

    override fun observeSettings(): Flow<TriggerSettings> = store.settings

    override suspend fun currentSettings(): TriggerSettings = store.current()

    override suspend fun setEnabled(type: TriggerType, enabled: Boolean) = update { settings ->
        settings.mapTrigger(type) { it.copy(enabled = enabled && it.available) }
    }

    override suspend fun setSensitivity(type: TriggerType, sensitivity: Int) = update { settings ->
        settings.mapTrigger(type) { it.copy(sensitivity = sensitivity.coerceIn(0, 100)) }
    }

    override suspend fun setPanicCode(code: String) = update { it.copy(panicCode = code) }

    override suspend fun setCountdownSeconds(seconds: Int) =
        update { it.copy(countdownSeconds = seconds.coerceIn(0, 30)) }

    override suspend fun setDisguise(disguise: PanicDisguise) =
        update { it.copy(disguise = disguise) }

    override suspend fun setDuressPinAlsoAlerts(enabled: Boolean) =
        update { it.copy(duressPinAlsoAlerts = enabled) }

    private suspend inline fun update(transform: (TriggerSettings) -> TriggerSettings) {
        store.write(transform(store.current()))
    }
}

internal fun TriggerSettings.mapTrigger(
    type: TriggerType,
    transform: (TriggerConfig) -> TriggerConfig,
): TriggerSettings = copy(triggers = triggers.map { if (it.type == type) transform(it) else it })
