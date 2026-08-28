package com.utils.calc.navigation

import androidx.lifecycle.ViewModel
import com.utils.calc.core.domain.session.VaultSession
import com.utils.calc.feature.panic.PanicController
import com.utils.calc.feature.panic.PanicState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Vive acima do NavHost porque o disfarce de emergência precisa cobrir qualquer
 * tela, inclusive o cofre aberto.
 */
@HiltViewModel
class AppPanicViewModel @Inject constructor(
    private val panicController: PanicController,
    private val vaultSession: VaultSession,
) : ViewModel() {

    val state: StateFlow<PanicState> = panicController.state

    fun onCancel() = panicController.cancel()

    fun onCloseVault() = vaultSession.close()
}
