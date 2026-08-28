package com.utils.calc.feature.vault.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utils.calc.R
import com.utils.calc.core.designsystem.component.ContactFormDialog
import com.utils.calc.core.designsystem.component.ContactRow
import com.utils.calc.core.designsystem.component.EmptyState
import com.utils.calc.core.designsystem.component.InfoNote
import com.utils.calc.core.designsystem.component.SectionCard
import com.utils.calc.core.designsystem.component.VaultScaffold
import com.utils.calc.core.designsystem.theme.Spacing
import com.utils.calc.core.domain.model.TrustedContact
import com.utils.calc.core.domain.repository.ContactsRepository

@Composable
fun ContactsRoute(
    onBack: () -> Unit,
    onQuickExit: () -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ContactsScreen(
        state = state,
        onBack = onBack,
        onQuickExit = {
            viewModel.onCloseSession()
            onQuickExit()
        },
        onSave = viewModel::onSave,
        onDelete = viewModel::onDelete,
        onSetPrimary = viewModel::onSetPrimary,
        onMessageChanged = viewModel::onMessageChanged,
    )
}

@Composable
fun ContactsScreen(
    state: ContactsUiState,
    onBack: () -> Unit,
    onQuickExit: () -> Unit,
    onSave: (String?, String, String, String) -> Unit,
    onDelete: (String) -> Unit,
    onSetPrimary: (String) -> Unit,
    onMessageChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<TrustedContact?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TrustedContact?>(null) }

    VaultScaffold(
        title = stringResource(R.string.contacts_title),
        onQuickExit = onQuickExit,
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionCard {
                if (state.contacts.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Group,
                        title = stringResource(R.string.contacts_empty_title),
                        description = stringResource(R.string.contacts_empty_body),
                    )
                } else {
                    state.contacts.forEachIndexed { index, contact ->
                        if (index > 0) HorizontalDivider()
                        ContactRow(
                            name = contact.name,
                            phone = contact.phone,
                            relationship = contact.relationship,
                            initials = contact.initials,
                            isPrimary = contact.isPrimary,
                        ) {
                            if (!contact.isPrimary) {
                                IconButton(onClick = { onSetPrimary(contact.id) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.StarOutline,
                                        contentDescription =
                                            stringResource(R.string.contacts_set_primary),
                                    )
                                }
                            }
                            IconButton(onClick = { editing = contact }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.action_edit),
                                )
                            }
                            IconButton(onClick = { pendingDelete = contact }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.action_delete),
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { creating = true },
                    enabled = state.canAdd,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = Icons.Outlined.PersonAdd, contentDescription = null)
                    Text(
                        text = stringResource(R.string.contacts_add),
                        modifier = Modifier.padding(start = Spacing.sm),
                    )
                }

                if (!state.canAdd) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.contacts_limit,
                            ContactsRepository.MAX_CONTACTS,
                            ContactsRepository.MAX_CONTACTS,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionCard(
                title = stringResource(R.string.contacts_message_title),
                subtitle = stringResource(R.string.contacts_message_help),
            ) {
                OutlinedTextField(
                    value = state.alertMessage,
                    onValueChange = onMessageChanged,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Text(
                    text = stringResource(R.string.contacts_message_preview, state.previewMessage()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            InfoNote(text = stringResource(R.string.contacts_phone_notice))
        }
    }

    if (creating) {
        ContactFormDialog(
            onDismiss = { creating = false },
            onConfirm = { name, phone, relationship ->
                onSave(null, name, phone, relationship)
                creating = false
            },
        )
    }

    editing?.let { contact ->
        ContactFormDialog(
            initialName = contact.name,
            initialPhone = contact.phone,
            initialRelationship = contact.relationship,
            onDismiss = { editing = null },
            onConfirm = { name, phone, relationship ->
                onSave(contact.id, name, phone, relationship)
                editing = null
            },
        )
    }

    pendingDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.contacts_delete_title)) },
            text = { Text(stringResource(R.string.contacts_delete_body, contact.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(contact.id)
                        pendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
