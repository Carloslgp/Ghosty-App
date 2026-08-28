package com.utils.calc.core.data.fake

import com.utils.calc.core.domain.model.EvidenceItem
import com.utils.calc.core.domain.repository.EvidenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda os itens em memoria. Nenhum arquivo e' gravado nesta fase — a gravacao
 * local de verdade e' a Fase 3 do roadmap. A tela de evidencias existe agora
 * para que o fluxo e a navegacao possam ser exercitados.
 */
@Singleton
class FakeEvidenceRepository @Inject constructor() : EvidenceRepository {

    private val items = MutableStateFlow(emptyList<EvidenceItem>())

    override fun observeAll(): Flow<List<EvidenceItem>> = items.asStateFlow()

    override suspend fun add(item: EvidenceItem) {
        items.update { current -> (current + item).sortedByDescending { it.createdAt } }
    }

    override suspend fun delete(id: String) {
        items.update { current -> current.filterNot { it.id == id } }
    }

    override suspend fun deleteAll() {
        items.value = emptyList()
    }
}
