package com.utils.calc.core.domain.repository

import com.utils.calc.core.domain.model.PanicDisguise
import com.utils.calc.core.domain.model.TriggerSettings
import com.utils.calc.core.domain.model.TriggerType
import kotlinx.coroutines.flow.Flow

interface TriggerRepository {
    fun observeSettings(): Flow<TriggerSettings>

    suspend fun currentSettings(): TriggerSettings

    suspend fun setEnabled(type: TriggerType, enabled: Boolean)

    suspend fun setSensitivity(type: TriggerType, sensitivity: Int)

    suspend fun setPanicCode(code: String)

    suspend fun setCountdownSeconds(seconds: Int)

    suspend fun setDisguise(disguise: PanicDisguise)

    suspend fun setDuressPinAlsoAlerts(enabled: Boolean)
}
