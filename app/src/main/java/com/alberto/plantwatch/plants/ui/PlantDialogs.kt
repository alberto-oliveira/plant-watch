package com.alberto.plantwatch.plants.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.alberto.plantwatch.R
import com.alberto.plantwatch.ui.theme.PlantWatchTheme

/**
 * Dialog to type a new plant's name. Stateless like the list screen: the text
 * being typed (`name`) is owned by the caller, and every keystroke is reported
 * through `onNameChange`, which then passes the new text back in.
 *
 * This "value + onValueChange" pair is how all Compose inputs work: the text
 * field never stores text by itself, it always shows what it's given.
 */
@Composable
fun AddPlantDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        // Called when tapping outside the dialog or pressing Back.
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_plant_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.add_plant_name_label)) },
                singleLine = true,
                // Start each word with a capital letter ("Snake Plant").
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        },
        confirmButton = {
            // `isNotBlank()` is false for "" and for whitespace-only text, so the
            // button can't add a nameless plant. (The ViewModel checks again.)
            TextButton(onClick = onConfirm, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.add_plant_confirm))
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
 * Asks for confirmation before deleting a plant, naming it and warning that its
 * tasks and history go too. The Delete button uses the theme's "error" color
 * to signal a destructive action.
 */
@Composable
fun DeletePlantDialog(
    plantName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_plant_title, plantName)) },
        text = { Text(stringResource(R.string.delete_plant_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.delete_plant_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

// --- Previews ---

@Preview
@Composable
private fun AddPlantDialogPreview() {
    PlantWatchTheme {
        AddPlantDialog(name = "Monstera", onNameChange = {}, onConfirm = {}, onDismiss = {})
    }
}

@Preview
@Composable
private fun DeletePlantDialogPreview() {
    PlantWatchTheme {
        DeletePlantDialog(plantName = "Monstera", onConfirm = {}, onDismiss = {})
    }
}
