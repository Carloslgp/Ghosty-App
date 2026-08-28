package com.utils.calc.core.domain.model

sealed interface RecordingState {
    data object Idle : RecordingState

    data class Recording(
        val startedAt: Long,
        val elapsedMillis: Long,
        val kind: EvidenceKind,
    ) : RecordingState

    data class Saved(val evidenceId: String) : RecordingState

    data class Failed(val reason: String) : RecordingState
}
