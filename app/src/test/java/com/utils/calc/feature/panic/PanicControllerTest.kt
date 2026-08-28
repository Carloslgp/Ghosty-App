package com.utils.calc.feature.panic

import app.cash.turbine.test
import com.utils.calc.core.domain.model.TriggerSettings
import com.utils.calc.testing.FakeContactsRepository
import com.utils.calc.testing.FakeLocationProvider
import com.utils.calc.testing.FakeRecordingService
import com.utils.calc.testing.FakeTimeSource
import com.utils.calc.testing.FakeTriggerRepository
import com.utils.calc.testing.RecordingAlertDispatcher
import com.utils.calc.testing.contact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PanicControllerTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)

    private val timeSource = FakeTimeSource()
    private val contacts = FakeContactsRepository(
        listOf(contact("1", "Ana", primary = true), contact("2", "Bia")),
    )
    private val triggers = FakeTriggerRepository(
        TriggerSettings.Default.copy(countdownSeconds = 0),
    )
    private val alertDispatcher = RecordingAlertDispatcher(timeSource)
    private val recording = FakeRecordingService()
    private val location = FakeLocationProvider()

    private fun controller() = PanicController(
        scope = scope,
        contactsRepository = contacts,
        triggerRepository = triggers,
        alertDispatcher = alertDispatcher,
        recordingService = recording,
        locationProvider = location,
        timeSource = timeSource,
    )

    @Test
    fun `acionamento avisa todos os contatos e fica ativo`() = runTest(dispatcher) {
        val controller = controller()
        controller.trigger(PanicSource.MANUAL)
        advanceUntilIdle()

        val state = controller.state.value
        assertEquals(PanicPhase.Active, state.phase)
        assertEquals(2, alertDispatcher.requests.size)
        assertEquals(1, recording.startCount)
        assertTrue(state.log.any { it.event is PanicEvent.Triggered })
        assertTrue(state.log.any { it.event is PanicEvent.LocationCaptured })
    }

    @Test
    fun `contato principal e avisado primeiro`() = runTest(dispatcher) {
        val controller = controller()
        controller.trigger(PanicSource.MANUAL)
        advanceUntilIdle()

        assertEquals("Ana", alertDispatcher.requests.first().contact.name)
    }

    @Test
    fun `cancelar durante a contagem nao avisa ninguem`() = runTest(dispatcher) {
        triggers.settings.value = TriggerSettings.Default.copy(countdownSeconds = 10)
        val controller = controller()

        controller.trigger(PanicSource.MANUAL)
        scheduler.advanceTimeBy(2_000)
        assertTrue(controller.state.value.phase is PanicPhase.Countdown)

        controller.cancel()
        advanceUntilIdle()

        assertEquals(PanicPhase.Cancelled, controller.state.value.phase)
        assertTrue(alertDispatcher.requests.isEmpty())
        assertEquals(0, recording.startCount)
    }

    @Test
    fun `ensaio nao grava e termina sozinho`() = runTest(dispatcher) {
        val controller = controller()
        controller.trigger(PanicSource.SAFE_TEST, simulated = true)
        advanceUntilIdle()

        val state = controller.state.value
        assertEquals(PanicPhase.Finished, state.phase)
        assertEquals(0, recording.startCount)
        assertTrue(state.simulated)
        assertTrue(alertDispatcher.requests.all { it.simulated })
    }

    @Test
    fun `ensaio nao toma a tela`() = runTest(dispatcher) {
        val controller = controller()
        controller.trigger(PanicSource.SAFE_TEST, simulated = true)
        scheduler.runCurrent()

        assertFalse(controller.state.value.showsOverlay)
    }

    @Test
    fun `codigo de coacao avisa uma vez, sem tela e sem gravacao aberta`() = runTest(dispatcher) {
        triggers.settings.value = TriggerSettings.Default.copy(countdownSeconds = 5)
        val controller = controller()

        controller.trigger(PanicSource.DURESS_PIN)
        advanceUntilIdle()

        val state = controller.state.value
        assertFalse(state.showsOverlay)
        // Sem espera: nao ha disfarce na tela, logo nao ha como cancelar.
        assertFalse(state.log.any { it.event is PanicEvent.CountdownStarted })
        assertEquals(2, alertDispatcher.requests.size)
        assertEquals(0, recording.startCount)
        assertEquals(PanicPhase.Finished, state.phase)
    }

    @Test
    fun `sem contatos o fluxo continua e registra o motivo`() = runTest(dispatcher) {
        contacts.contacts.value = emptyList()
        val controller = controller()

        controller.trigger(PanicSource.MANUAL)
        advanceUntilIdle()

        assertTrue(alertDispatcher.requests.isEmpty())
        assertTrue(controller.state.value.log.any { it.event is PanicEvent.NoContacts })
        assertEquals(1, recording.startCount)
    }

    @Test
    fun `acionar de novo enquanto ativo nao reinicia o fluxo`() = runTest(dispatcher) {
        val controller = controller()
        controller.trigger(PanicSource.MANUAL)
        advanceUntilIdle()

        controller.trigger(PanicSource.LONG_PRESS)
        advanceUntilIdle()

        assertEquals(2, alertDispatcher.requests.size)
        assertEquals(PanicSource.MANUAL, controller.state.value.source)
    }

    @Test
    fun `cancelar com a emergencia ativa encerra a gravacao`() = runTest(dispatcher) {
        val controller = controller()
        controller.trigger(PanicSource.MANUAL)
        advanceUntilIdle()

        controller.cancel()
        advanceUntilIdle()

        assertEquals(PanicPhase.Finished, controller.state.value.phase)
        assertEquals(1, recording.stopCount)
    }

    @Test
    fun `estado emite a transicao completa`() = runTest(dispatcher) {
        triggers.settings.value = TriggerSettings.Default.copy(countdownSeconds = 1)
        val controller = controller()

        controller.state.test {
            assertEquals(PanicPhase.Idle, awaitItem().phase)
            controller.trigger(PanicSource.CALCULATOR_CODE)
            advanceUntilIdle()

            val phases = mutableListOf<PanicPhase>()
            while (phases.lastOrNull() !is PanicPhase.Active) {
                phases += awaitItem().phase
            }
            assertTrue(phases.any { it is PanicPhase.Countdown })
            assertTrue(phases.any { it is PanicPhase.Dispatching })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
