package com.utils.calc.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppSkin { Facade, Vault }

/** Cores que o Material 3 nao cobre e que precisam existir nos dois temas. */
data class ExtraColors(
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val keypadDigit: Color,
    val keypadFunction: Color,
)

private val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(
        danger = Palette.Danger,
        onDanger = Palette.OnDanger,
        dangerContainer = Palette.DangerContainer,
        onDangerContainer = Palette.OnDangerContainer,
        warning = Palette.Warning,
        warningContainer = Palette.WarningContainer,
        keypadDigit = Palette.FacadeSurface,
        keypadFunction = Palette.FacadeSurfaceVariant,
    )
}

object AppTheme {
    val extra: ExtraColors
        @Composable @ReadOnlyComposable get() = LocalExtraColors.current
}

private val FacadeLight = lightColorScheme(
    primary = Palette.FacadeAccent,
    onPrimary = Palette.FacadeOnAccent,
    primaryContainer = Palette.FacadeSurfaceVariant,
    onPrimaryContainer = Palette.FacadeOnSurface,
    secondary = Palette.FacadeAccent,
    onSecondary = Palette.FacadeOnAccent,
    background = Palette.FacadeBackground,
    onBackground = Palette.FacadeOnSurface,
    surface = Palette.FacadeSurface,
    onSurface = Palette.FacadeOnSurface,
    surfaceVariant = Palette.FacadeSurfaceVariant,
    onSurfaceVariant = Palette.FacadeOnSurfaceVariant,
    outline = Palette.FacadeOutline,
    error = Palette.Danger,
    onError = Palette.OnDanger,
)

private val FacadeDark = darkColorScheme(
    primary = Palette.FacadeAccentDark,
    onPrimary = Palette.FacadeOnAccentDark,
    primaryContainer = Palette.FacadeSurfaceVariantDark,
    onPrimaryContainer = Palette.FacadeOnSurfaceDark,
    secondary = Palette.FacadeAccentDark,
    onSecondary = Palette.FacadeOnAccentDark,
    background = Palette.FacadeBackgroundDark,
    onBackground = Palette.FacadeOnSurfaceDark,
    surface = Palette.FacadeSurfaceDark,
    onSurface = Palette.FacadeOnSurfaceDark,
    surfaceVariant = Palette.FacadeSurfaceVariantDark,
    onSurfaceVariant = Palette.FacadeOnSurfaceVariantDark,
    outline = Palette.FacadeOutlineDark,
    error = Palette.DangerDark,
    onError = Palette.OnDangerDark,
)

private val VaultLight = lightColorScheme(
    primary = Palette.VaultPrimary,
    onPrimary = Palette.VaultOnPrimary,
    primaryContainer = Palette.VaultPrimaryContainer,
    onPrimaryContainer = Palette.VaultOnPrimaryContainer,
    secondary = Palette.VaultPrimary,
    onSecondary = Palette.VaultOnPrimary,
    secondaryContainer = Palette.VaultSurfaceVariant,
    onSecondaryContainer = Palette.VaultOnSurface,
    background = Palette.VaultBackground,
    onBackground = Palette.VaultOnSurface,
    surface = Palette.VaultSurface,
    onSurface = Palette.VaultOnSurface,
    surfaceVariant = Palette.VaultSurfaceVariant,
    onSurfaceVariant = Palette.VaultOnSurfaceVariant,
    outline = Palette.VaultOutline,
    error = Palette.Danger,
    onError = Palette.OnDanger,
    errorContainer = Palette.DangerContainer,
    onErrorContainer = Palette.OnDangerContainer,
)

private val VaultDark = darkColorScheme(
    primary = Palette.VaultPrimaryDark,
    onPrimary = Palette.VaultOnPrimaryDark,
    primaryContainer = Palette.VaultPrimaryContainerDark,
    onPrimaryContainer = Palette.VaultOnPrimaryContainerDark,
    secondary = Palette.VaultPrimaryDark,
    onSecondary = Palette.VaultOnPrimaryDark,
    secondaryContainer = Palette.VaultSurfaceVariantDark,
    onSecondaryContainer = Palette.VaultOnSurfaceDark,
    background = Palette.VaultBackgroundDark,
    onBackground = Palette.VaultOnSurfaceDark,
    surface = Palette.VaultSurfaceDark,
    onSurface = Palette.VaultOnSurfaceDark,
    surfaceVariant = Palette.VaultSurfaceVariantDark,
    onSurfaceVariant = Palette.VaultOnSurfaceVariantDark,
    outline = Palette.VaultOutlineDark,
    error = Palette.DangerDark,
    onError = Palette.OnDangerDark,
    errorContainer = Palette.DangerContainerDark,
    onErrorContainer = Palette.OnDangerContainerDark,
)

@Composable
fun CalcTheme(
    skin: AppSkin = AppSkin.Facade,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        skin == AppSkin.Vault && darkTheme -> VaultDark
        skin == AppSkin.Vault -> VaultLight
        darkTheme -> FacadeDark
        else -> FacadeLight
    }

    val extra = ExtraColors(
        danger = if (darkTheme) Palette.DangerDark else Palette.Danger,
        onDanger = if (darkTheme) Palette.OnDangerDark else Palette.OnDanger,
        dangerContainer = if (darkTheme) Palette.DangerContainerDark else Palette.DangerContainer,
        onDangerContainer = if (darkTheme) Palette.OnDangerContainerDark else Palette.OnDangerContainer,
        warning = if (darkTheme) Palette.WarningDark else Palette.Warning,
        warningContainer = if (darkTheme) Palette.WarningContainerDark else Palette.WarningContainer,
        keypadDigit = if (darkTheme) Palette.FacadeSurfaceDark else Palette.FacadeSurface,
        keypadFunction = if (darkTheme) Palette.FacadeSurfaceVariantDark else Palette.FacadeSurfaceVariant,
    )

    CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
