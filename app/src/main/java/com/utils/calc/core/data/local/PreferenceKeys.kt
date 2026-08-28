package com.utils.calc.core.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Nomes deliberadamente inexpressivos: quem inspecionar o arquivo nao entende do que se trata. */
internal object PreferenceKeys {
    val SetupDone = booleanPreferencesKey("cfg_done")
    val PinAccess = stringPreferencesKey("cfg_a")
    val PinDuress = stringPreferencesKey("cfg_b")
    val Contacts = stringPreferencesKey("cfg_c")
    val AlertMessage = stringPreferencesKey("cfg_m")
    val Triggers = stringPreferencesKey("cfg_t")
}
