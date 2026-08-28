package com.utils.calc.core.data.repository

import com.utils.calc.core.data.local.ContactsLocalStore
import com.utils.calc.core.domain.model.TrustedContact
import com.utils.calc.core.domain.repository.ContactsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val store: ContactsLocalStore,
) : ContactsRepository {

    override fun observeContacts(): Flow<List<TrustedContact>> = store.contacts

    override fun observeAlertMessage(): Flow<String> = store.alertMessage

    override suspend fun upsert(contact: TrustedContact) {
        val current = store.current()
        store.write(current.upsert(contact))
    }

    override suspend fun delete(contactId: String) {
        store.write(store.current().withoutContact(contactId))
    }

    override suspend fun setPrimary(contactId: String) {
        store.write(store.current().withPrimary(contactId))
    }

    override suspend fun setAlertMessage(message: String) {
        store.writeMessage(message)
    }
}

internal fun List<TrustedContact>.upsert(contact: TrustedContact): List<TrustedContact> {
    val replaced = if (any { it.id == contact.id }) {
        map { if (it.id == contact.id) contact else it }
    } else {
        this + contact
    }
    // Sempre exatamente um contato principal: e' o primeiro a ser avisado.
    return when {
        contact.isPrimary -> replaced.map { it.copy(isPrimary = it.id == contact.id) }
        replaced.none { it.isPrimary } -> replaced.mapIndexed { i, c -> c.copy(isPrimary = i == 0) }
        else -> replaced
    }
}

internal fun List<TrustedContact>.withoutContact(contactId: String): List<TrustedContact> {
    val remaining = filterNot { it.id == contactId }
    return if (remaining.isNotEmpty() && remaining.none { it.isPrimary }) {
        remaining.mapIndexed { i, c -> c.copy(isPrimary = i == 0) }
    } else {
        remaining
    }
}

internal fun List<TrustedContact>.withPrimary(contactId: String): List<TrustedContact> =
    map { it.copy(isPrimary = it.id == contactId) }
