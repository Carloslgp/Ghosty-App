package com.utils.calc.core.domain.model

enum class EvidenceKind { AUDIO, VIDEO, LOCATION }

/**
 * Item de evidencia guardado **localmente**. Nesta fase nada e' gravado de fato:
 * os itens sao criados pela camada fake para que a tela e o fluxo existam.
 */
data class EvidenceItem(
    val id: String,
    val kind: EvidenceKind,
    val createdAt: Long,
    val durationMillis: Long,
    val sizeBytes: Long,
    val location: GeoPoint?,
    val placeholder: Boolean,
)
