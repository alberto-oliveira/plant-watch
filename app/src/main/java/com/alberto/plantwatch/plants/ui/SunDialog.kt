package com.alberto.plantwatch.plants.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alberto.plantwatch.R
import com.alberto.plantwatch.ui.theme.PlantWatchTheme
import com.alberto.plantwatch.weather.parseSunSettings

/**
 * Dialog for a plant's sun tracking: exposure % and the hour window. Stateless
 * (texts in, changes out); Save is enabled only for values accepted by
 * [parseSunSettings] (unit-tested). "Turn off" is shown only when tracking is on.
 */
@Composable
fun SunDialog(
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
    val valid = parseSunSettings(startHour, endHour, exposure) != null
    // Positive whole numbers only, so the number keyboard is fine here.
    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sun_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

@Preview
@Composable
private fun SunDialogPreview() {
    PlantWatchTheme {
        SunDialog(
            exposure = "40", onExposureChange = {},
            startHour = "8", onStartHourChange = {},
            endHour = "18", onEndHourChange = {},
            isTracked = true,
            onConfirm = {}, onTurnOff = {}, onDismiss = {},
        )
    }
}
