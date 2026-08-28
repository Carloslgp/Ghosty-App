package com.utils.calc.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityLocalStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val setupDone: Flow<Boolean> = dataStore.data.map { it[PreferenceKeys.SetupDone] == true }

    suspend fun accessHash(): String? = dataStore.data.first()[PreferenceKeys.PinAccess]

    suspend fun duressHash(): String? = dataStore.data.first()[PreferenceKeys.PinDuress]

    suspend fun writeAccessHash(hash: String) {
        dataStore.edit { it[PreferenceKeys.PinAccess] = hash }
    }

    suspend fun writeDuressHash(hash: String) {
        dataStore.edit { it[PreferenceKeys.PinDuress] = hash }
    }

    suspend fun markSetupDone() {
        dataStore.edit { it[PreferenceKeys.SetupDone] = true }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
