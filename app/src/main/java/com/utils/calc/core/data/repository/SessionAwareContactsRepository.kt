package com.utils.calc.core.data.repository

import com.utils.calc.core.data.fake.DuressDataStore
import com.utils.calc.core.domain.model.TrustedContact
import com.utils.calc.core.domain.model.VaultMode
import com.utils.calc.core.domain.repository.ContactsRepository
import com.utils.calc.core.domain.session.VaultSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SessionAwareContactsRepository @Inject constructor(
    private val real: ContactsRepositoryImpl,
    private val duress: DuressDataStore,
    private val session: VaultSession,
) : ContactsRepository {

    override fun observeContacts(): Flow<List<TrustedContact>> =
        session.mode.flatMapLatest { mode ->
            if (mode == VaultMode.DURESS) duress.contacts else real.observeContacts()
        }

    override fun observeAlertMessage(): Flow<String> =
        session.mode.flatMapLatest { mode ->
            if (mode == VaultMode.DURESS) duress.alertMessage else real.observeAlertMessage()
        }

    override suspend fun upsert(contact: TrustedContact) = onDuress(
        duressAction = { duress.writeContacts(duress.contacts.value.upsert(contact)) },
        realAction = { real.upsert(contact) },
    )

    override suspend fun delete(contactId: String) = onDuress(
        duressAction = { duress.writeContacts(duress.contacts.value.withoutContact(contactId)) },
        realAction = { real.delete(contactId) },
    )

    override suspend fun setPrimary(contactId: String) = onDuress(
        duressAction = { duress.writeContacts(duress.contacts.value.withPrimary(contactId)) },
        realAction = { real.setPrimary(contactId) },
    )

    override suspend fun setAlertMessage(message: String) = onDuress(
        duressAction = { duress.writeAlertMessage(message) },
        realAction = { real.setAlertMessage(message) },
    )

    private suspend inline fun onDuress(duressAction: () -> Unit, realAction: () -> Unit) {
        if (session.activeMode == VaultMode.DURESS) duressAction() else realAction()
    }
}
