package com.utils.calc.feature.calculator

import app.cash.turbine.test
import com.utils.calc.core.domain.model.TriggerSettings
import com.utils.calc.core.domain.model.TriggerType
import com.utils.calc.core.domain.model.VaultMode
import com.utils.calc.feature.calculator.engine.CalcKey
import com.utils.calc.feature.calculator.engine.CalcOperator
import com.utils.calc.feature.panic.PanicController
import com.utils.calc.feature.panic.PanicSource
import com.utils.calc.testing.FakeContactsRepository
import com.utils.calc.testing.FakeLocationProvider
import com.utils.calc.testing.FakeRecordingService
import com.utils.calc.testing.FakeSecurityRepository
import com.utils.calc.testing.FakeTimeSource
import com.utils.calc.testing.FakeTriggerRepository
import com.utils.calc.testing.FakeVaultSession
import com.utils.calc.testing.MainDispatcherRule
import com.utils.calc.testing.RecordingAlertDispatcher
import com.utils.calc.testing.contact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val triggers = FakeTriggerRepository(
        TriggerSettings.Default.copy(panicCode = "7788", countdownSeconds = 0),
    )
    private val security = FakeSecurityRepository(triggerRepository = triggers)
    private val session = FakeVaultSession()
    private val timeSource = FakeTimeSource()
    private val alertDispatcher = RecordingAlertDispatcher(timeSource)

    private val panicController = PanicController(
        scope = CoroutineScope(mainDispatcherRule.dispatcher),
        contactsRepository = FakeContactsRepository(listOf(contact("1", "Ana", primary = true))),
        triggerRepository = triggers,
        alertDispatcher = alertDispatcher,
        recordingService = FakeRecordingService(),
        locationProvider = FakeLocationProvider(),
        timeSource = timeSource,
    )

    private fun viewModel() = CalculatorViewModel(
        securityRepository = security,
        triggerRepository = triggers,
        vaultSession = session,
        panicController = panicController,
    )

    private suspend fun CalculatorViewModel.type(digits: String) {
        digits.forEach { onKey(CalcKey.Digit(it.digitToInt())) }
    }

    private suspend fun configured() {
        security.setPin(com.utils.calc.core.domain.model.PinKind.ACCESS, "4713")
        security.setPin(com.utils.calc.core.domain.model.PinKind.DURESS, "2096")
        security.completeOnboarding()
    }

    @Test
    fun `digitos aparecem no visor`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.type("1234")
            advanceUntilIdle()
            assertEquals("1.234", expectMostRecentItem().display)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `conta comum continua funcionando`() = runTest {
        configured()
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.type("12")
            viewModel.onKey(CalcKey.Operation(CalcOperator.Add))
            viewModel.type("30")
            viewModel.onKey(CalcKey.Equals)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("42", state.display)
            assertEquals(1, state.history.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `PIN de acesso abre o cofre real e nao deixa rastro`() = runTest {
        configured()
        val viewModel = viewModel()

        viewModel.uiEvents.test {
            viewModel.type("4713")
            viewModel.onKey(CalcKey.Equals)
            advanceUntilIdle()

            assertEquals(CalculatorEvent.OpenVault, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(VaultMode.REAL, session.mode.value)
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("0", state.display)
            assertTrue(state.history.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `PIN de coacao abre o cofre falso`() = runTest {
        configured()
        val viewModel = viewModel()

        viewModel.uiEvents.test {
            viewModel.type("2096")
            viewModel.onKey(CalcKey.Equals)
            advanceUntilIdle()

            assertEquals(CalculatorEvent.OpenVault, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(VaultMode.DURESS, session.mode.value)
        assertTrue(alertDispatcher.requests.isEmpty())
    }

    @Test
    fun `PIN de coacao alerta em silencio quando a opcao esta ligada`() = runTest {
        configured()
        triggers.setDuressPinAlsoAlerts(true)
        val viewModel = viewModel()

        viewModel.type("2096")
        viewModel.onKey(CalcKey.Equals)
        advanceUntilIdle()

        assertEquals(VaultMode.DURESS, session.mode.value)
        assertEquals(1, alertDispatcher.requests.size)
        assertEquals(PanicSource.DURESS_PIN, panicController.state.value.source)
    }

    @Test
    fun `codigo de emergencia dispara sem abrir o cofre`() = runTest {
        configured()
        val viewModel = viewModel()

        viewModel.type("7788")
        viewModel.onKey(CalcKey.Equals)
        advanceUntilIdle()

        assertNull(session.mode.value)
        assertEquals(PanicSource.CALCULATOR_CODE, panicController.state.value.source)

        viewModel.uiState.test {
            assertEquals("0", expectMostRecentItem().display)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `codigo desconhecido apenas calcula`() = runTest {
        configured()
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.type("9999")
            viewModel.onKey(CalcKey.Equals)
            advanceUntilIdle()

            assertEquals("9.999", expectMostRecentItem().display)
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(session.mode.value)
        assertTrue(alertDispatcher.requests.isEmpty())
    }

    @Test
    fun `gatilho por codigo desligado nao dispara`() = runTest {
        configured()
        triggers.setEnabled(TriggerType.CALCULATOR_CODE, false)
        val viewModel = viewModel()

        viewModel.type("7788")
        viewModel.onKey(CalcKey.Equals)
        advanceUntilIdle()

        assertTrue(alertDispatcher.requests.isEmpty())
        assertNull(panicController.state.value.source)
    }

    @Test
    fun `antes da configuracao o codigo inicial abre o onboarding`() = runTest {
        val viewModel = viewModel()

        viewModel.uiEvents.test {
            viewModel.type(TriggerSettings.SETUP_CODE)
            viewModel.onKey(CalcKey.Equals)
            advanceUntilIdle()

            assertEquals(CalculatorEvent.OpenOnboarding, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toque longo no zero abre o onboarding antes da configuracao`() = runTest {
        val viewModel = viewModel()

        viewModel.uiEvents.test {
            viewModel.onZeroLongPress()
            advanceUntilIdle()

            assertEquals(CalculatorEvent.OpenOnboarding, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toque longo no zero dispara a emergencia depois da configuracao`() = runTest {
        configured()
        val viewModel = viewModel()

        viewModel.onZeroLongPress()
        advanceUntilIdle()

        assertEquals(PanicSource.LONG_PRESS, panicController.state.value.source)
    }

    @Test
    fun `toque longo no zero nao dispara com o gatilho desligado`() = runTest {
        configured()
        triggers.setEnabled(TriggerType.KEY_LONG_PRESS, false)
        val viewModel = viewModel()

        viewModel.onZeroLongPress()
        advanceUntilIdle()

        assertNull(panicController.state.value.source)
    }

    @Test
    fun `codigo dentro de uma conta nao dispara nada`() = runTest {
        configured()
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.type("7000")
            viewModel.onKey(CalcKey.Operation(CalcOperator.Add))
            viewModel.type("788")
            viewModel.onKey(CalcKey.Equals)
            advanceUntilIdle()

            assertEquals("7.788", expectMostRecentItem().display)
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(panicController.state.value.source)
        assertNull(session.mode.value)
    }
}
