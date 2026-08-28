package com.utils.calc.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.utils.calc.core.data.local.crypto.LocalCipher
import com.utils.calc.core.data.local.dto.TriggerSettingsDto
import com.utils.calc.core.data.local.dto.toDomain
import com.utils.calc.core.data.local.dto.toDto
import com.utils.calc.core.domain.model.TriggerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggersLocalStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: LocalCipher,
) {
    val settings: Flow<TriggerSettings> = dataStore.data.map { prefs ->
        decode(prefs[PreferenceKeys.Triggers])
    }

    suspend fun current(): TriggerSettings = settings.first()

    suspend fun write(settings: TriggerSettings) {
        val payload = cipher.encrypt(LocalJson.encodeToString(settings.toDto()))
        dataStore.edit { it[PreferenceKeys.Triggers] = payload }
    }

    private fun decode(encoded: String?): TriggerSettings {
        val json = cipher.decryptOrNull(encoded) ?: return TriggerSettings.Default
        return runCatching {
            LocalJson.decodeFromString<TriggerSettingsDto>(json).toDomain()
        }.getOrDefault(TriggerSettings.Default)
    }
}
