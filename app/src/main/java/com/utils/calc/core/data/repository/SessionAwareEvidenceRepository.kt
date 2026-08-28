package com.utils.calc.core.data.repository

import com.utils.calc.core.data.fake.DuressDataStore
import com.utils.calc.core.data.fake.FakeEvidenceRepository
import com.utils.calc.core.domain.model.EvidenceItem
import com.utils.calc.core.domain.model.VaultMode
import com.utils.calc.core.domain.repository.EvidenceRepository
import com.utils.calc.core.domain.session.VaultSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SessionAwareEvidenceRepository @Inject constructor(
    private val real: FakeEvidenceRepository,
    private val duress: DuressDataStore,
    private val session: VaultSession,
) : EvidenceRepository {

    override fun observeAll(): Flow<List<EvidenceItem>> =
        session.mode.flatMapLatest { mode ->
            if (mode == VaultMode.DURESS) duress.evidence else real.observeAll()
        }

    override suspend fun add(item: EvidenceItem) {
        if (isDuress) duress.writeEvidence(duress.evidence.value + item) else real.add(item)
    }

    override suspend fun delete(id: String) {
        if (isDuress) {
            duress.writeEvidence(duress.evidence.value.filterNot { it.id == id })
        } else {
            real.delete(id)
        }
    }

    override suspend fun deleteAll() {
        if (isDuress) duress.writeEvidence(emptyList()) else real.deleteAll()
    }

    private val isDuress: Boolean
        get() = session.activeMode == VaultMode.DURESS
}
