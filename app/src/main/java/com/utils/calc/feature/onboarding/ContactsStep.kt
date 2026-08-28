package com.utils.calc.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.utils.calc.R
import com.utils.calc.core.designsystem.component.ContactFormDialog
import com.utils.calc.core.designsystem.component.ContactRow
import com.utils.calc.core.designsystem.component.InfoNote
import com.utils.calc.core.designsystem.component.SectionCard
import com.utils.calc.core.designsystem.theme.Spacing
import com.utils.calc.core.domain.repository.ContactsRepository

@Composable
fun ColumnScope.ContactsStep(
    state: OnboardingUiState,
    onAddContact: (String, String, String) -> Unit,
    onRemoveContact: (String) -> Unit,
    onContinue: () -> Unit,
) {
    var showForm by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.onb_contacts_title),
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(
        text = pluralStringResource(
            R.plurals.onb_contacts_subtitle,
            ContactsRepository.MAX_CONTACTS,
            ContactsRepository.MAX_CONTACTS,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    SectionCard {
        if (state.contacts.isEmpty()) {
            Text(
                text = stringResource(R.string.onb_contacts_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column {
                state.contacts.forEach { contact ->
                    ContactRow(
                        name = contact.name,
                        phone = contact.phone,
                        relationship = contact.relationship,
                        initials = contact.initials,
                        isPrimary = contact.isPrimary,
                    ) {
                        IconButton(onClick = { onRemoveContact(contact.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { showForm = true },
            enabled = state.canAddContact,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Outlined.PersonAdd, contentDescription = null)
            Text(
                text = stringResource(R.string.contacts_add),
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
    }

    InfoNote(text = stringResource(R.string.contacts_phone_notice))

    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.action_next))
    }

    if (showForm) {
        ContactFormDialog(
            onDismiss = { showForm = false },
            onConfirm = { name, phone, relationship ->
                onAddContact(name, phone, relationship)
                showForm = false
            },
        )
    }
}
