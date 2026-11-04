package com.alberto.plantwatch.plants.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alberto.plantwatch.R
import com.alberto.plantwatch.guide.SpeciesMatch
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
    // Optional species suggestions for the typed name (empty = none shown).
    speciesSuggestions: List<SpeciesMatch> = emptyList(),
    selectedSpeciesId: String? = null,
    onSpeciesSelect: (String?) -> Unit = {},
) {
    AlertDialog(
        // Called when tapping outside the dialog or pressing Back.
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_plant_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.add_plant_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        // Start each word with a capital letter ("Snake Plant").
                        capitalization = KeyboardCapitalization.Words,
                        // Show a "Done" (✓) key instead of Enter...
                        imeAction = ImeAction.Done,
                    ),
                    // ...and make it add the plant, same as the Add button. Useful in
                    // landscape, where the keyboard covers the dialog's buttons.
                    keyboardActions = KeyboardActions(
                        onDone = { if (name.isNotBlank()) onConfirm() },
                    ),
                )
                // Species suggestions as chips; tapping the selected one again clears it.
                // Picking a species is optional: the plant can always be added without.
                if (speciesSuggestions.isNotEmpty()) {
                    Text(stringResource(R.string.add_plant_species_hint), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        speciesSuggestions.forEach { match ->
                            val selected = match.entry.id == selectedSpeciesId
                            FilterChip(
                                selected = selected,
                                onClick = { onSpeciesSelect(if (selected) null else match.entry.id) },
                                label = { Text("${displayName(match.entry)} · ${match.entry.scientificName}") },
                            )
                        }
                    }
                }
            }
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
