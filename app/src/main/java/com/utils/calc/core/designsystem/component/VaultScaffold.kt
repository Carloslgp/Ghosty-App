package com.utils.calc.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.utils.calc.BuildConfig
import com.utils.calc.R
import com.utils.calc.core.designsystem.theme.AppTheme
import com.utils.calc.core.designsystem.theme.Spacing

/**
 * Moldura comum das telas do cofre: FLAG_SECURE, titulo, volta e — sempre
 * visivel — a saida rapida, que devolve a calculadora e limpa a pilha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScaffold(
    title: String,
    onQuickExit: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    SecureScreen()
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = title) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                )
                            }
                        }
                    },
                    actions = {
                        actions()
                        QuickExitButton(onQuickExit)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
                if (BuildConfig.DRY_RUN) DevModeStrip()
            }
        },
        content = content,
    )
}

@Composable
fun QuickExitButton(onQuickExit: () -> Unit) {
    TextButton(onClick = onQuickExit) {
        Icon(
            imageVector = Icons.Outlined.GridView,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.action_quick_exit),
            modifier = Modifier.padding(start = Spacing.xs),
        )
    }
}

/**
 * Aviso honesto de fase. Enquanto DRY_RUN estiver ligado nada sai do aparelho, e
 * a usuaria precisa saber disso antes de contar com o app.
 */
@Composable
fun DevModeStrip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.extra.warningContainer)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.Science,
            contentDescription = null,
            tint = AppTheme.extra.warning,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.dev_mode_strip),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.extra.warning,
        )
    }
}
