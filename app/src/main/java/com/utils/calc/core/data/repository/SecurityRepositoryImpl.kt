package com.utils.calc.core.data.repository

import com.utils.calc.core.data.local.SecurityLocalStore
import com.utils.calc.core.data.local.crypto.PinHasher
import com.utils.calc.core.domain.model.CodeMatch
import com.utils.calc.core.domain.model.PinKind
import com.utils.calc.core.domain.model.TriggerType
import com.utils.calc.core.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepositoryImpl @Inject constructor(
    private val store: SecurityLocalStore,
    private val triggers: TriggerRepositoryImpl,
) : SecurityRepository {

    override fun observeOnboardingCompleted(): Flow<Boolean> = store.setupDone

    override suspend fun isOnboardingCompleted(): Boolean = store.setupDone.first()

    override suspend fun setPin(kind: PinKind, pin: String) {
        val hash = PinHasher.hash(pin)
        when (kind) {
            PinKind.ACCESS -> store.writeAccessHash(hash)
            PinKind.DURESS -> store.writeDuressHash(hash)
        }
    }

    override suspend fun hasPin(kind: PinKind): Boolean = when (kind) {
        PinKind.ACCESS -> store.accessHash() != null
        PinKind.DURESS -> store.duressHash() != null
    }

    /**
     * O PIN vence o codigo de panico em caso de colisao: abrir o cofre e' a acao
     * reversivel: disparar o alerta nao e'.
     */
    override suspend fun match(code: String): CodeMatch {
        if (!isOnboardingCompleted()) return CodeMatch.None
        if (PinHasher.verify(code, store.accessHash())) return CodeMatch.Pin(PinKind.ACCESS)
        if (PinHasher.verify(code, store.duressHash())) return CodeMatch.Pin(PinKind.DURESS)

        val settings = triggers.currentSettings()
        val panicArmed = settings.isEnabled(TriggerType.CALCULATOR_CODE)
        if (panicArmed && settings.panicCode.isNotBlank() && code == settings.panicCode) {
            return CodeMatch.PanicCode
        }
        return CodeMatch.None
    }

    override suspend fun completeOnboarding() = store.markSetupDone()

    override suspend fun reset() = store.clear()
}
