package com.utils.calc.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.utils.calc.R
import com.utils.calc.core.designsystem.component.InfoNote
import com.utils.calc.core.designsystem.component.NoteTone
import com.utils.calc.core.designsystem.component.NumericKeypad
import com.utils.calc.core.designsystem.component.PinDots
import com.utils.calc.core.domain.model.PinRules
import com.utils.calc.core.designsystem.theme.Spacing

@Composable
fun ColumnScope.PinStep(
    state: OnboardingUiState,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onContinue: () -> Unit,
) {
    val isAccess = state.step == OnboardingStep.AccessPin
    val title = when {
        isAccess && !state.confirming -> R.string.onb_access_title
        isAccess -> R.string.onb_access_confirm_title
        !state.confirming -> R.string.onb_duress_title
        else -> R.string.onb_duress_confirm_title
    }

    Text(text = stringResource(title), style = MaterialTheme.typography.headlineSmall)

    if (!state.confirming) {
        Text(
            text = stringResource(
                if (isAccess) R.string.onb_access_subtitle else R.string.onb_duress_subtitle,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PinDots(
            filled = state.pin.length,
            total = PinRules.MAX_LENGTH,
            error = state.problem != null,
            accessibilityLabel = pluralStringResource(
                R.plurals.onb_pin_dots_description,
                state.pin.length,
                state.pin.length,
            ),
        )

        Text(
            text = stringResource(R.string.onb_pin_hint_length),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.problem?.let { problem ->
            InfoNote(
                text = stringResource(problem.messageRes()),
                tone = NoteTone.Danger,
            )
        }

        NumericKeypad(onDigit = onDigit, onBackspace = onBackspace)
    }

    Button(
        onClick = onContinue,
        enabled = state.canAdvancePin,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.action_next))
    }
}

private fun PinProblem.messageRes(): Int = when (this) {
    PinProblem.TooShort -> R.string.onb_pin_error_short
    PinProblem.TooObvious -> R.string.onb_pin_error_obvious
    PinProblem.Mismatch -> R.string.onb_pin_error_mismatch
    PinProblem.SameAsAccess -> R.string.onb_pin_error_same_as_access
}
