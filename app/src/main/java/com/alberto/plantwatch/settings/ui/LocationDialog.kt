package com.alberto.plantwatch.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alberto.plantwatch.R
import com.alberto.plantwatch.ui.theme.PlantWatchTheme
import com.alberto.plantwatch.weather.parseLocation
import java.util.Locale

/**
 * Dialog to type the location used for sunlight data (stateless: the two texts
 * come in, every change goes out). Save is enabled only for a valid pair, as
 * checked by [parseLocation] (unit-tested).
 *
 * The fields use the normal keyboard: Android's number keyboards often have no
 * minus sign, and southern/western coordinates are negative.
 */
@Composable
fun LocationDialog(
    latitude: String,
    onLatitudeChange: (String) -> Unit,
    longitude: String,
    onLongitudeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val valid = parseLocation(latitude, longitude) != null
    // Show the error only once both fields have something in them.
    val showError = latitude.isNotBlank() && longitude.isNotBlank() && !valid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.location_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.location_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = latitude,
                    onValueChange = onLatitudeChange,
                    label = { Text(stringResource(R.string.location_latitude)) },
                    singleLine = true,
                    isError = showError,
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = onLongitudeChange,
                    label = { Text(stringResource(R.string.location_longitude)) },
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.location_invalid)) }
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
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Coordinates as shown in settings, e.g. "-23.5505, -46.6333". `Locale.ROOT`
 * forces a dot as decimal separator whatever the phone's language, so the text
 * looks like (and can be typed back as) a coordinate.
 */
fun formatCoordinate(value: Double): String = String.format(Locale.ROOT, "%.4f", value)

@Preview
@Composable
private fun LocationDialogPreview() {
    PlantWatchTheme {
        LocationDialog(
            latitude = "-23.5505", onLatitudeChange = {},
            longitude = "-46.6333", onLongitudeChange = {},
            onConfirm = {}, onDismiss = {},
        )
    }
}
