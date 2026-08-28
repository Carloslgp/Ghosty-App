package com.utils.calc.core.domain.repository

import com.utils.calc.core.domain.model.TrustedContact
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {
    fun observeContacts(): Flow<List<TrustedContact>>

    /** Mensagem que seria enviada aos contatos. Suporta os marcadores {nome} e {local}. */
    fun observeAlertMessage(): Flow<String>

    suspend fun upsert(contact: TrustedContact)

    suspend fun delete(contactId: String)

    suspend fun setPrimary(contactId: String)

    suspend fun setAlertMessage(message: String)

    companion object {
        const val DEFAULT_MESSAGE =
            "{nome}, estou em perigo e preciso de ajuda agora. Minha ultima localizacao: {local}"
        const val MAX_CONTACTS = 5
    }
}
