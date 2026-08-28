package com.utils.calc.core.domain.model

enum class TriggerType {
    CALCULATOR_CODE,
    KEY_LONG_PRESS,
    QUICK_SETTINGS_TILE,
    POWER_BUTTON,
    VOLUME_KEYS,
    SHAKE,
}

/**
 * [available] marca gatilhos que ainda nao existem nesta fase. Eles aparecem na
 * lista, desabilitados, para que a usuaria saiba o que esta' e o que nao esta'
 * funcionando — um gatilho que finge estar ligado e' pior do que gatilho nenhum.
 */
data class TriggerConfig(
    val type: TriggerType,
    val enabled: Boolean,
    val sensitivity: Int = DEFAULT_SENSITIVITY,
    val available: Boolean = true,
) {
    companion object {
        const val DEFAULT_SENSITIVITY = 50
    }
}

data class TriggerSettings(
    val triggers: List<TriggerConfig>,
    /** Codigo que, digitado na calculadora seguido de "=", dispara o panico. */
    val panicCode: String,
    /** Segundos de contagem regressiva antes do disparo efetivo. 0 = imediato. */
    val countdownSeconds: Int,
    /** Aparencia que a tela assume durante o panico. */
    val disguise: PanicDisguise,
    /** Se o PIN de coacao tambem dispara um alerta silencioso ao abrir o cofre falso. */
    val duressPinAlsoAlerts: Boolean,
) {
    fun isEnabled(type: TriggerType): Boolean =
        triggers.firstOrNull { it.type == type }?.let { it.enabled && it.available } == true

    fun sensitivityOf(type: TriggerType): Int =
        triggers.firstOrNull { it.type == type }?.sensitivity ?: TriggerConfig.DEFAULT_SENSITIVITY

    companion object {
        const val DEFAULT_PANIC_CODE = "1984"

        /**
         * Antes do onboarding nao existe PIN nenhum, entao este codigo e' a unica
         * porta de entrada para a configuracao. Depois dele configurado, o mesmo
         * valor passa a ser apenas o codigo de panico padrao, editavel.
         */
        const val SETUP_CODE = DEFAULT_PANIC_CODE

        const val DEFAULT_COUNTDOWN_SECONDS = 5

        val Default = TriggerSettings(
            triggers = listOf(
                TriggerConfig(TriggerType.CALCULATOR_CODE, enabled = true),
                TriggerConfig(TriggerType.KEY_LONG_PRESS, enabled = true),
                TriggerConfig(TriggerType.QUICK_SETTINGS_TILE, enabled = false, available = false),
                TriggerConfig(TriggerType.POWER_BUTTON, enabled = false, available = false),
                TriggerConfig(TriggerType.VOLUME_KEYS, enabled = false, available = false),
                TriggerConfig(TriggerType.SHAKE, enabled = false, sensitivity = 35, available = false),
            ),
            panicCode = DEFAULT_PANIC_CODE,
            countdownSeconds = DEFAULT_COUNTDOWN_SECONDS,
            disguise = PanicDisguise.CALCULATOR,
            duressPinAlsoAlerts = false,
        )
    }
}
