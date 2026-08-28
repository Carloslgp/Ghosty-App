package com.utils.calc.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.utils.calc.R
import com.utils.calc.core.designsystem.component.InfoNote
import com.utils.calc.core.designsystem.component.NoteTone
import com.utils.calc.core.designsystem.component.SectionCard
import com.utils.calc.core.designsystem.theme.Spacing

/**
 * As permissões são pedidas de verdade, mas nada nesta fase as usa: a gravação e
 * a localização são simuladas. O passo existe agora para que o texto honesto
 * sobre a notificação de gravação seja lido antes de ela contar com o app.
 */
@Composable
fun ColumnScope.PermissionsStep(
    saving: Boolean,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val required = remember { requiredPermissions() }
    var granted by remember {
        mutableStateOf(required.filter { context.hasPermission(it) }.toSet())
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        granted = granted + result.filterValues { it }.keys
    }

    Text(
        text = stringResource(R.string.onb_permissions_title),
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(
        text = stringResource(R.string.onb_permissions_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PermissionRow(
                label = stringResource(R.string.onb_permission_audio),
                granted = Manifest.permission.RECORD_AUDIO in granted,
            )
            PermissionRow(
                label = stringResource(R.string.onb_permission_location),
                granted = Manifest.permission.ACCESS_FINE_LOCATION in granted,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionRow(
                    label = stringResource(R.string.onb_permission_notifications),
                    granted = Manifest.permission.POST_NOTIFICATIONS in granted,
                )
            }
        }

        OutlinedButton(
            onClick = { launcher.launch(required.toTypedArray()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onb_permissions_request))
        }
    }

    InfoNote(
        title = stringResource(R.string.onb_honesty_title),
        text = stringResource(R.string.onb_honesty_body),
        tone = NoteTone.Warning,
    )

    InfoNote(
        title = stringResource(R.string.onb_visibility_title),
        text = stringResource(R.string.onb_visibility_body),
    )

    InfoNote(
        title = stringResource(R.string.onb_done_title),
        text = stringResource(R.string.onb_done_body),
    )

    Button(
        onClick = onFinish,
        enabled = !saving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.action_finish))
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = if (granted) {
                Icons.Outlined.CheckCircle
            } else {
                Icons.Outlined.RadioButtonUnchecked
            },
            contentDescription = stringResource(
                if (granted) R.string.onb_permission_granted else R.string.onb_permission_denied,
            ),
            tint = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = Spacing.xs),
        )
    }
}

private fun requiredPermissions(): List<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private fun android.content.Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
