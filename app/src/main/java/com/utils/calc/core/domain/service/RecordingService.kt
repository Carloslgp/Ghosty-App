package com.utils.calc.core.domain.service

import com.utils.calc.core.domain.model.EvidenceKind
import com.utils.calc.core.domain.model.RecordingState
import kotlinx.coroutines.flow.StateFlow

interface RecordingService {
    val state: StateFlow<RecordingState>

    suspend fun start(kind: EvidenceKind)

    /** Encerra a gravacao e devolve o id da evidencia gerada, se houver. */
    suspend fun stop(): String?
}
