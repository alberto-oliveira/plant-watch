package com.alberto.plantwatch.plants.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alberto.plantwatch.R
import com.alberto.plantwatch.ui.theme.PlantWatchTheme
import com.alberto.plantwatch.weather.Compass
import com.alberto.plantwatch.weather.WindowDistance
import com.alberto.plantwatch.weather.WindowPlacement
import com.alberto.plantwatch.weather.WindowSettings
import com.alberto.plantwatch.weather.parseSunSettings

/** How a plant's sun is worked out. */
enum class SunMode { WINDOW, HOURS }

/**
 * Dialog for a plant's sun tracking, in two modes:
 * - **By window**: chips for where the window faces, how far the plant is and
 *   whether it stands in front of the window or to the side.
 * - **Custom hours**: exposure % and an hour window (e.g. for outdoor plants).
 *
 * Stateless: the current choices come in, every change goes out. Window mode is
 * always valid (every chip combination makes sense); custom hours are checked
 * with [parseSunSettings] (unit-tested). "Turn off" is shown only when tracking
 * is on.
 */
@Composable
fun SunDialog(
    mode: SunMode,
    onModeChange: (SunMode) -> Unit,
    window: WindowSettings,
    onWindowChange: (WindowSettings) -> Unit,
    exposure: String,
    onExposureChange: (String) -> Unit,
    startHour: String,
    onStartHourChange: (String) -> Unit,
    endHour: String,
    onEndHourChange: (String) -> Unit,
    isTracked: Boolean,
    onConfirm: () -> Unit,
    onTurnOff: () -> Unit,
    onDismiss: () -> Unit,
) {
    val valid = mode == SunMode.WINDOW || parseSunSettings(startHour, endHour, exposure) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sun_dialog_title)) },
        text = {
            // The window chips make the dialog tall: let it scroll on small screens.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SingleChoiceSegmentedButtonRow {
                    // `SunMode.entries`: all values of the enum, in order.
                    SunMode.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == mode,
                            onClick = { onModeChange(option) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = SunMode.entries.size),
                        ) {
                            Text(
                                stringResource(if (option == SunMode.WINDOW) R.string.sun_mode_window else R.string.sun_mode_hours),
                            )
                        }
                    }
                }

                // `when` on an enum: one branch per mode.
                when (mode) {
                    SunMode.WINDOW -> WindowFields(window, onWindowChange)
                    SunMode.HOURS -> HourFields(
                        exposure, onExposureChange, startHour, onStartHourChange, endHour, onEndHourChange, valid,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = valid) {
                Text(stringResource(R.string.save))
            }
        },
        // The dismiss slot can hold any composable: here a Row with two buttons.
        dismissButton = {
            Row {
                if (isTracked) {
                    TextButton(onClick = onTurnOff) {
                        Text(stringResource(R.string.sun_turn_off))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )
}

/**
 * The window pickers: three groups of chips. `FlowRow` places its children in a
 * row and wraps onto the next line when they don't fit (8 compass chips don't).
 * Each chip reports a change with `window.copy(...)`, replacing just one field.
 */
@Composable
private fun WindowFields(window: WindowSettings, onWindowChange: (WindowSettings) -> Unit) {
    Text(stringResource(R.string.sun_window_hint), style = MaterialTheme.typography.bodyMedium)

    ChipGroupTitle(stringResource(R.string.sun_window_faces))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Compass.entries.forEach { direction ->
            FilterChip(
                selected = direction == window.direction,
                onClick = { onWindowChange(window.copy(direction = direction)) },
                label = { Text(compassName(direction)) },
            )
        }
    }

    ChipGroupTitle(stringResource(R.string.sun_plant_distance))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WindowDistance.entries.forEach { distance ->
            FilterChip(
                selected = distance == window.distance,
                onClick = { onWindowChange(window.copy(distance = distance)) },
                label = { Text(distanceName(distance)) },
            )
        }
    }

    ChipGroupTitle(stringResource(R.string.sun_plant_placement))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WindowPlacement.entries.forEach { placement ->
            FilterChip(
                selected = placement == window.placement,
                onClick = { onWindowChange(window.copy(placement = placement)) },
                label = { Text(placementName(placement)) },
            )
        }
    }
}

@Composable
private fun ChipGroupTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

/** The custom-hours fields (unchanged from before the window mode existed). */
@Composable
private fun HourFields(
    exposure: String,
    onExposureChange: (String) -> Unit,
    startHour: String,
    onStartHourChange: (String) -> Unit,
    endHour: String,
    onEndHourChange: (String) -> Unit,
    valid: Boolean,
) {
    // Positive whole numbers only, so the number keyboard is fine here.
    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
    Text(stringResource(R.string.sun_dialog_hint), style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(
        value = exposure,
        onValueChange = onExposureChange,
        label = { Text(stringResource(R.string.sun_exposure_label)) },
        singleLine = true,
        keyboardOptions = numberKeyboard,
        isError = !valid,
    )
    OutlinedTextField(
        value = startHour,
        onValueChange = onStartHourChange,
        label = { Text(stringResource(R.string.sun_start_label)) },
        singleLine = true,
        keyboardOptions = numberKeyboard,
        isError = !valid,
    )
    OutlinedTextField(
        value = endHour,
        onValueChange = onEndHourChange,
        label = { Text(stringResource(R.string.sun_end_label)) },
        singleLine = true,
        keyboardOptions = numberKeyboard,
        isError = !valid,
        supportingText = if (!valid) {
            { Text(stringResource(R.string.sun_invalid)) }
        } else {
            null
        },
    )
}

/** "North", "South-east", ... */
@Composable
fun compassName(direction: Compass): String = stringResource(
    when (direction) {
        Compass.N -> R.string.compass_n
        Compass.NE -> R.string.compass_ne
        Compass.E -> R.string.compass_e
        Compass.SE -> R.string.compass_se
        Compass.S -> R.string.compass_s
        Compass.SW -> R.string.compass_sw
        Compass.W -> R.string.compass_w
        Compass.NW -> R.string.compass_nw
    },
)

/** "On the sill", "1–2 m", ... */
@Composable
fun distanceName(distance: WindowDistance): String = stringResource(
    when (distance) {
        WindowDistance.SILL -> R.string.distance_sill
        WindowDistance.UNDER_1M -> R.string.distance_under_1m
        WindowDistance.FROM_1_TO_2M -> R.string.distance_1_to_2m
        WindowDistance.OVER_2M -> R.string.distance_over_2m
    },
)

/** "In front", "To the side". */
@Composable
fun placementName(placement: WindowPlacement): String = stringResource(
    when (placement) {
        WindowPlacement.IN_FRONT -> R.string.placement_in_front
        WindowPlacement.TO_THE_SIDE -> R.string.placement_to_the_side
    },
)

@Preview
@Composable
private fun SunDialogWindowPreview() {
    PlantWatchTheme {
        SunDialog(
            mode = SunMode.WINDOW, onModeChange = {},
            window = WindowSettings(Compass.N, WindowDistance.FROM_1_TO_2M, WindowPlacement.IN_FRONT), onWindowChange = {},
            exposure = "100", onExposureChange = {}, startHour = "8", onStartHourChange = {},
            endHour = "18", onEndHourChange = {},
            isTracked = true, onConfirm = {}, onTurnOff = {}, onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun SunDialogHoursPreview() {
    PlantWatchTheme {
        SunDialog(
            mode = SunMode.HOURS, onModeChange = {},
            window = WindowSettings(Compass.N, WindowDistance.SILL, WindowPlacement.IN_FRONT), onWindowChange = {},
            exposure = "40", onExposureChange = {}, startHour = "8", onStartHourChange = {},
            endHour = "18", onEndHourChange = {},
            isTracked = false, onConfirm = {}, onTurnOff = {}, onDismiss = {},
        )
    }
}
