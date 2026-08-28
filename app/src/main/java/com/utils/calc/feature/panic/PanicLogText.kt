package com.utils.calc.feature.panic

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.utils.calc.R
import com.utils.calc.core.domain.model.PanicDisguise
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun panicSourceLabel(source: PanicSource): String = stringResource(
    when (source) {
        PanicSource.MANUAL -> R.string.panic_source_manual
        PanicSource.CALCULATOR_CODE -> R.string.panic_source_code
        PanicSource.LONG_PRESS -> R.string.panic_source_long_press
        PanicSource.DURESS_PIN -> R.string.panic_source_duress
        PanicSource.SAFE_TEST -> R.string.panic_source_safe_test
    },
)

@Composable
fun disguiseLabel(disguise: PanicDisguise): String = stringResource(
    when (disguise) {
        PanicDisguise.CALCULATOR -> R.string.triggers_disguise_calculator
        PanicDisguise.BLACK_SCREEN -> R.string.triggers_disguise_black
        PanicDisguise.LOW_BATTERY -> R.string.triggers_disguise_battery
    },
)

/**
 * Traduz o registro do fluxo para uma linha legível. É o que substitui, nesta
 * fase, qualquer envio de verdade: a usuária vê exatamente o que aconteceu.
 */
@Composable
fun panicEventText(event: PanicEvent): String = when (event) {
    is PanicEvent.Triggered ->
        stringResource(R.string.safe_test_step_trigger, panicSourceLabel(event.source))

    is PanicEvent.CountdownStarted ->
        stringResource(R.string.safe_test_step_countdown, event.seconds)

    is PanicEvent.DisguiseApplied ->
        stringResource(R.string.safe_test_step_disguise, disguiseLabel(event.disguise))

    PanicEvent.RecordingStarted -> stringResource(R.string.safe_test_step_recording)

    is PanicEvent.LocationCaptured -> stringResource(
        R.string.safe_test_step_location,
        formatCoordinates(event.point.latitude, event.point.longitude),
    )

    is PanicEvent.AlertDispatched ->
        stringResource(R.string.safe_test_step_alert, event.outcome.contactName)

    PanicEvent.NoContacts -> stringResource(R.string.safe_test_no_contacts)
    PanicEvent.Cancelled -> stringResource(R.string.panic_cancelled)
    PanicEvent.Finished -> stringResource(R.string.safe_test_step_done)
}

fun formatCoordinates(latitude: Double, longitude: Double): String =
    String.format(Locale.getDefault(), "%.5f, %.5f", latitude, longitude)

fun formatClock(millis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f kB", bytes / 1024.0)
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
}
