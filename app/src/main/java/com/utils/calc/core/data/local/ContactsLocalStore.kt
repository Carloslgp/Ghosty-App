package com.utils.calc.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.utils.calc.core.data.local.crypto.LocalCipher
import com.utils.calc.core.data.local.dto.ContactDto
import com.utils.calc.core.data.local.dto.toDomain
import com.utils.calc.core.data.local.dto.toDto
import com.utils.calc.core.domain.model.TrustedContact
import com.utils.calc.core.domain.repository.ContactsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsLocalStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: LocalCipher,
) {
    val contacts: Flow<List<TrustedContact>> = dataStore.data.map { prefs ->
        decode(prefs[PreferenceKeys.Contacts])
    }

    val alertMessage: Flow<String> = dataStore.data.map { prefs ->
        cipher.decryptOrNull(prefs[PreferenceKeys.AlertMessage]) ?: ContactsRepository.DEFAULT_MESSAGE
    }

    suspend fun current(): List<TrustedContact> = contacts.first()

    suspend fun write(list: List<TrustedContact>) {
        val payload = cipher.encrypt(LocalJson.encodeToString(list.map { it.toDto() }))
        dataStore.edit { it[PreferenceKeys.Contacts] = payload }
    }

    suspend fun writeMessage(message: String) {
        val payload = cipher.encrypt(message)
        dataStore.edit { it[PreferenceKeys.AlertMessage] = payload }
    }

    private fun decode(encoded: String?): List<TrustedContact> {
        val json = cipher.decryptOrNull(encoded) ?: return emptyList()
        return runCatching {
            LocalJson.decodeFromString<List<ContactDto>>(json).map { it.toDomain() }
        }.getOrDefault(emptyList())
    }
}
