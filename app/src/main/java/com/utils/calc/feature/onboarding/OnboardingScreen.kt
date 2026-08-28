package com.utils.calc.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utils.calc.R
import com.utils.calc.core.designsystem.component.SecureScreen
import com.utils.calc.core.designsystem.theme.Spacing

@Composable
fun OnboardingRoute(
    onCompleted: () -> Unit,
    onAbandoned: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                OnboardingEvent.Completed -> onCompleted()
                OnboardingEvent.Abandoned -> onAbandoned()
            }
        }
    }

    BackHandler { viewModel.onBack() }

    OnboardingScreen(
        state = state,
        onDigit = viewModel::onDigit,
        onBackspace = viewModel::onBackspace,
        onPinContinue = viewModel::onPinContinue,
        onAddContact = viewModel::onAddContact,
        onRemoveContact = viewModel::onRemoveContact,
        onContactsContinue = viewModel::onContactsContinue,
        onFinish = viewModel::onFinish,
        onBack = viewModel::onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onPinContinue: () -> Unit,
    onAddContact: (String, String, String) -> Unit,
    onRemoveContact: (String) -> Unit,
    onContactsContinue: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SecureScreen()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(
                                R.string.onb_step_counter,
                                state.stepNumber,
                                OnboardingStep.entries.size,
                            ),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
                LinearProgressIndicator(
                    progress = { state.stepNumber / OnboardingStep.entries.size.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        OnboardingContent(
            state = state,
            padding = padding,
            onDigit = onDigit,
            onBackspace = onBackspace,
            onPinContinue = onPinContinue,
            onAddContact = onAddContact,
            onRemoveContact = onRemoveContact,
            onContactsContinue = onContactsContinue,
            onFinish = onFinish,
        )
    }
}

@Composable
private fun OnboardingContent(
    state: OnboardingUiState,
    padding: PaddingValues,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onPinContinue: () -> Unit,
    onAddContact: (String, String, String) -> Unit,
    onRemoveContact: (String) -> Unit,
    onContactsContinue: () -> Unit,
    onFinish: () -> Unit,
) {
    val scrollable = state.step == OnboardingStep.Contacts || state.step == OnboardingStep.Permissions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        when (state.step) {
            OnboardingStep.AccessPin,
            OnboardingStep.DuressPin,
            -> PinStep(
                state = state,
                onDigit = onDigit,
                onBackspace = onBackspace,
                onContinue = onPinContinue,
            )

            OnboardingStep.Contacts -> ContactsStep(
                state = state,
                onAddContact = onAddContact,
                onRemoveContact = onRemoveContact,
                onContinue = onContactsContinue,
            )

            OnboardingStep.Permissions -> PermissionsStep(
                saving = state.saving,
                onFinish = onFinish,
            )
        }
    }
}
