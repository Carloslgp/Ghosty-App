package com.utils.calc.core.domain.repository

import com.utils.calc.core.domain.model.EvidenceItem
import kotlinx.coroutines.flow.Flow

interface EvidenceRepository {
    fun observeAll(): Flow<List<EvidenceItem>>

    suspend fun add(item: EvidenceItem)

    suspend fun delete(id: String)

    suspend fun deleteAll()
}
