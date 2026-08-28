package com.utils.calc.core.data.fake

import com.utils.calc.core.di.ApplicationScope
import com.utils.calc.core.domain.model.EvidenceItem
import com.utils.calc.core.domain.model.EvidenceKind
import com.utils.calc.core.domain.model.RecordingState
import com.utils.calc.core.domain.repository.EvidenceRepository
import com.utils.calc.core.domain.service.LocationProvider
import com.utils.calc.core.domain.service.RecordingService
import com.utils.calc.core.domain.service.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simula a gravacao: conta o tempo decorrido e, ao parar, cria um item de
 * evidencia marcado como [EvidenceItem.placeholder]. Nenhum microfone e' aberto
 * e nenhum arquivo e' escrito — isso e' a Fase 3 do roadmap.
 */
@Singleton
class FakeRecordingService @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val evidence: EvidenceRepository,
    private val locationProvider: LocationProvider,
    private val timeSource: TimeSource,
) : RecordingService {

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var ticker: Job? = null

    override suspend fun start(kind: EvidenceKind) {
        if (_state.value is RecordingState.Recording) return
        val startedAt = timeSource.nowMillis()
        _state.value = RecordingState.Recording(startedAt, elapsedMillis = 0L, kind = kind)
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                val current = _state.value
                if (current !is RecordingState.Recording) break
                _state.value = current.copy(elapsedMillis = timeSource.nowMillis() - startedAt)
            }
        }
    }

    override suspend fun stop(): String? {
        val current = _state.value as? RecordingState.Recording ?: run {
            _state.value = RecordingState.Idle
            return null
        }
        ticker?.cancel()
        ticker = null

        val id = UUID.randomUUID().toString()
        val duration = timeSource.nowMillis() - current.startedAt
        evidence.add(
            EvidenceItem(
                id = id,
                kind = current.kind,
                createdAt = current.startedAt,
                durationMillis = duration,
                sizeBytes = duration / 1000 * BYTES_PER_SECOND,
                location = locationProvider.current(),
                placeholder = true,
            ),
        )
        _state.value = RecordingState.Saved(id)
        return id
    }

    private companion object {
        const val TICK_MILLIS = 200L
        const val BYTES_PER_SECOND = 16_000L
    }
}
