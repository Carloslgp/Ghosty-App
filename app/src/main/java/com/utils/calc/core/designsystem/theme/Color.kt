package com.utils.calc.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Duas paletas. A da fachada precisa ser esquecivel: qualquer calculadora tem
 * essa cara. A do cofre pode ter identidade, porque so' e' vista por quem
 * digitou o codigo.
 */
internal object Palette {

    // Fachada — claro
    val FacadeBackground = Color(0xFFF3F4F7)
    val FacadeSurface = Color(0xFFFFFFFF)
    val FacadeSurfaceVariant = Color(0xFFE7EAF0)
    val FacadeOnSurface = Color(0xFF15171C)
    val FacadeOnSurfaceVariant = Color(0xFF5A6069)
    val FacadeAccent = Color(0xFF3D5A99)
    val FacadeOnAccent = Color(0xFFFFFFFF)
    val FacadeOutline = Color(0xFFCFD4DE)

    // Fachada — escuro
    val FacadeBackgroundDark = Color(0xFF0F1013)
    val FacadeSurfaceDark = Color(0xFF16181D)
    val FacadeSurfaceVariantDark = Color(0xFF23262D)
    val FacadeOnSurfaceDark = Color(0xFFECEDF1)
    val FacadeOnSurfaceVariantDark = Color(0xFF9AA1AC)
    val FacadeAccentDark = Color(0xFF93AEE8)
    val FacadeOnAccentDark = Color(0xFF10203F)
    val FacadeOutlineDark = Color(0xFF343841)

    // Cofre — claro
    val VaultBackground = Color(0xFFF5F8F7)
    val VaultSurface = Color(0xFFFFFFFF)
    val VaultSurfaceVariant = Color(0xFFDDE9E5)
    val VaultOnSurface = Color(0xFF11201C)
    val VaultOnSurfaceVariant = Color(0xFF48605A)
    val VaultPrimary = Color(0xFF0F6B5C)
    val VaultOnPrimary = Color(0xFFFFFFFF)
    val VaultPrimaryContainer = Color(0xFFB6EEE0)
    val VaultOnPrimaryContainer = Color(0xFF00201A)
    val VaultOutline = Color(0xFFB4C6C1)

    // Cofre — escuro
    val VaultBackgroundDark = Color(0xFF0C1412)
    val VaultSurfaceDark = Color(0xFF131C1A)
    val VaultSurfaceVariantDark = Color(0xFF1F2C29)
    val VaultOnSurfaceDark = Color(0xFFE2EFEB)
    val VaultOnSurfaceVariantDark = Color(0xFF9FB6B0)
    val VaultPrimaryDark = Color(0xFF5FD9C1)
    val VaultOnPrimaryDark = Color(0xFF00382F)
    val VaultPrimaryContainerDark = Color(0xFF005044)
    val VaultOnPrimaryContainerDark = Color(0xFFB6EEE0)
    val VaultOutlineDark = Color(0xFF3A4C48)

    // Emergencia — mesma familia nos dois temas para nao virar decoracao
    val Danger = Color(0xFFB3261E)
    val OnDanger = Color(0xFFFFFFFF)
    val DangerContainer = Color(0xFFF9DEDC)
    val OnDangerContainer = Color(0xFF410E0B)
    val DangerDark = Color(0xFFFFB4AB)
    val OnDangerDark = Color(0xFF690005)
    val DangerContainerDark = Color(0xFF93000A)
    val OnDangerContainerDark = Color(0xFFFFDAD6)

    val Warning = Color(0xFF8A5A00)
    val WarningContainer = Color(0xFFFFEBC7)
    val WarningDark = Color(0xFFFFD08A)
    val WarningContainerDark = Color(0xFF3D2A00)
}
