package com.utils.calc.core.data.fake

import com.utils.calc.core.domain.model.PanicDisguise
import com.utils.calc.core.domain.model.TriggerConfig
import com.utils.calc.core.domain.model.TriggerSettings
import com.utils.calc.core.domain.model.TriggerType
import com.utils.calc.core.domain.model.TrustedContact

/**
 * Conteudo do cofre de coacao. Precisa parecer um cofre em uso — mas configurado
 * ha' pouco tempo e sem nada dentro: contatos obvios, nenhuma gravacao, codigo
 * de panico que nao e' o verdadeiro. Um cofre vazio demais denuncia tanto quanto
 * um cofre cheio.
 */
object DuressSeed {

    val contacts: List<TrustedContact> = listOf(
        TrustedContact(
            id = "duress-1",
            name = "Mae",
            phone = "(11) 98888-0100",
            relationship = "Familia",
            isPrimary = true,
        ),
        TrustedContact(
            id = "duress-2",
            name = "Juliana",
            phone = "(11) 97777-0200",
            relationship = "Amiga",
            isPrimary = false,
        ),
    )

    const val ALERT_MESSAGE = "Preciso de ajuda, me liga assim que puder."

    val triggerSettings: TriggerSettings = TriggerSettings.Default.copy(
        triggers = TriggerSettings.Default.triggers.map { config ->
            when (config.type) {
                TriggerType.CALCULATOR_CODE -> config.copy(enabled = true)
                TriggerType.KEY_LONG_PRESS -> config.copy(enabled = false)
                else -> config
            }
        },
        panicCode = "2580",
        countdownSeconds = 5,
        disguise = PanicDisguise.CALCULATOR,
        duressPinAlsoAlerts = false,
    )

    fun defaultTrigger(type: TriggerType): TriggerConfig =
        triggerSettings.triggers.first { it.type == type }
}
