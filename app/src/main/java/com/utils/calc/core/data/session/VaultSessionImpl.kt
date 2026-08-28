package com.utils.calc.core.data.session

import com.utils.calc.core.data.fake.DuressDataStore
import com.utils.calc.core.domain.model.VaultMode
import com.utils.calc.core.domain.session.VaultSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultSessionImpl @Inject constructor(
    private val duressDataStore: DuressDataStore,
) : VaultSession {

    private val _mode = MutableStateFlow<VaultMode?>(null)
    override val mode: StateFlow<VaultMode?> = _mode.asStateFlow()

    override val activeMode: VaultMode
        get() = _mode.value ?: VaultMode.REAL

    override fun open(mode: VaultMode) {
        if (mode == VaultMode.DURESS) duressDataStore.reseed()
        _mode.value = mode
    }

    override fun close() {
        if (_mode.value == VaultMode.DURESS) duressDataStore.reseed()
        _mode.value = null
    }
}
