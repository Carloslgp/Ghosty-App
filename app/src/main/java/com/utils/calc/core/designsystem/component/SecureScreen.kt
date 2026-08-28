package com.utils.calc.core.designsystem.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Bloqueia captura de tela e a miniatura em Recentes. Vale para toda tela do
 * cofre: a miniatura de "apps recentes" e' onde o disfarce mais falha.
 */
@Composable
fun SecureScreen(enabled: Boolean = true) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val window = view.context.findActivity()?.window
        if (!enabled || window == null || view.isInEditMode) {
            onDispose { }
        } else {
            SecureFlagCounter.acquire(window)
            onDispose { SecureFlagCounter.release(window) }
        }
    }
}

/**
 * Navegar entre duas telas do cofre compoe a nova antes de descartar a antiga.
 * Sem contagem, o descarte da antiga limparia a flag que a nova acabou de pedir.
 */
private object SecureFlagCounter {
    private var count = 0

    fun acquire(window: Window) {
        count++
        if (count == 1) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
    }

    fun release(window: Window) {
        count = (count - 1).coerceAtLeast(0)
        if (count == 0) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
