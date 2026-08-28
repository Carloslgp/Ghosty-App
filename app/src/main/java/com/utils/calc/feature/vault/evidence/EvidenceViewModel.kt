package com.utils.calc.feature.vault.evidence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utils.calc.core.di.SessionAware
import com.utils.calc.core.domain.model.EvidenceItem
import com.utils.calc.core.domain.repository.EvidenceRepository
import com.utils.calc.core.domain.session.VaultSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaybackState(
    val itemId: String? = null,
    val positionMillis: Long = 0L,
)

data class EvidenceUiState(
    val items: List<EvidenceItem> = emptyList(),
    val playback: PlaybackState = PlaybackState(),
)

@HiltViewModel
class EvidenceViewModel @Inject constructor(
    @SessionAware private val evidenceRepository: EvidenceRepository,
    private val vaultSession: VaultSession,
) : ViewModel() {

    private val playback = MutableStateFlow(PlaybackState())
    private var playbackJob: Job? = null

    val uiState: StateFlow<EvidenceUiState> = combine(
        evidenceRepository.observeAll(),
        playback,
    ) { items, state ->
        EvidenceUiState(items = items, playback = state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = EvidenceUiState(),
    )

    /**
     * Não existe arquivo para tocar nesta fase: a barra apenas percorre a duração
     * registrada. A tela deixa isso explícito para não dar a entender que há
     * áudio guardado.
     */
    fun onTogglePlayback(item: EvidenceItem) {
        if (playback.value.itemId == item.id) {
            stopPlayback()
            return
        }
        playbackJob?.cancel()
        playback.value = PlaybackState(itemId = item.id, positionMillis = 0L)
        playbackJob = viewModelScope.launch {
            var position = 0L
            while (isActive && position < item.durationMillis) {
                delay(TICK_MILLIS)
                position += TICK_MILLIS
                playback.value = PlaybackState(item.id, position.coerceAtMost(item.durationMillis))
            }
            playback.value = PlaybackState()
        }
    }

    fun onDelete(id: String) {
        if (playback.value.itemId == id) stopPlayback()
        viewModelScope.launch { evidenceRepository.delete(id) }
    }

    fun onDeleteAll() {
        stopPlayback()
        viewModelScope.launch { evidenceRepository.deleteAll() }
    }

    fun onCloseSession() = vaultSession.close()

    private fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        playback.value = PlaybackState()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TICK_MILLIS = 200L
    }
}
