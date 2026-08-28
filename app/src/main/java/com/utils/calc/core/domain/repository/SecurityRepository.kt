package com.utils.calc.core.domain.repository

import com.utils.calc.core.domain.model.CodeMatch
import com.utils.calc.core.domain.model.PinKind
import kotlinx.coroutines.flow.Flow

interface SecurityRepository {
    fun observeOnboardingCompleted(): Flow<Boolean>

    suspend fun isOnboardingCompleted(): Boolean

    suspend fun setPin(kind: PinKind, pin: String)

    suspend fun hasPin(kind: PinKind): Boolean

    /** Compara o codigo digitado com os PINs e com o codigo de panico configurado. */
    suspend fun match(code: String): CodeMatch

    suspend fun completeOnboarding()

    /** Volta o app ao estado de primeira execucao. Usado no menu de desenvolvimento. */
    suspend fun reset()
}
