package com.utils.calc.core.data.local.dto

import com.utils.calc.core.domain.model.PanicDisguise
import com.utils.calc.core.domain.model.TriggerConfig
import com.utils.calc.core.domain.model.TriggerSettings
import com.utils.calc.core.domain.model.TriggerType
import com.utils.calc.core.domain.model.TrustedContact
import kotlinx.serialization.Serializable

@Serializable
internal data class ContactDto(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String,
    val isPrimary: Boolean,
)

internal fun ContactDto.toDomain() = TrustedContact(id, name, phone, relationship, isPrimary)

internal fun TrustedContact.toDto() = ContactDto(id, name, phone, relationship, isPrimary)

@Serializable
internal data class TriggerConfigDto(
    val type: String,
    val enabled: Boolean,
    val sensitivity: Int,
)

@Serializable
internal data class TriggerSettingsDto(
    val triggers: List<TriggerConfigDto> = emptyList(),
    val panicCode: String = TriggerSettings.DEFAULT_PANIC_CODE,
    val countdownSeconds: Int = TriggerSettings.DEFAULT_COUNTDOWN_SECONDS,
    val disguise: String = "CALCULATOR",
    val duressPinAlsoAlerts: Boolean = false,
)

internal fun TriggerSettings.toDto() = TriggerSettingsDto(
    triggers = triggers.map { TriggerConfigDto(it.type.name, it.enabled, it.sensitivity) },
    panicCode = panicCode,
    countdownSeconds = countdownSeconds,
    disguise = disguise.name,
    duressPinAlsoAlerts = duressPinAlsoAlerts,
)

/**
 * Reconstroi a partir do padrao para que gatilhos criados depois da gravacao
 * aparecam com a disponibilidade correta em vez de sumirem da lista.
 */
internal fun TriggerSettingsDto.toDomain(): TriggerSettings {
    val stored = triggers.mapNotNull { dto ->
        runCatching { TriggerType.valueOf(dto.type) }.getOrNull()?.let { it to dto }
    }.toMap()

    val merged = TriggerSettings.Default.triggers.map { default ->
        val dto = stored[default.type] ?: return@map default
        TriggerConfig(
            type = default.type,
            enabled = dto.enabled && default.available,
            sensitivity = dto.sensitivity.coerceIn(0, 100),
            available = default.available,
        )
    }

    return TriggerSettings(
        triggers = merged,
        panicCode = panicCode,
        countdownSeconds = countdownSeconds.coerceIn(0, 30),
        disguise = runCatching { PanicDisguise.valueOf(disguise) }.getOrDefault(PanicDisguise.CALCULATOR),
        duressPinAlsoAlerts = duressPinAlsoAlerts,
    )
}
