package com.alberto.plantwatch.plants.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alberto.plantwatch.R
import com.alberto.plantwatch.plants.data.TaskType
import com.alberto.plantwatch.ui.theme.PlantWatchTheme

/**
 * The interval typed by the user as a number of days, or null if it isn't a
 * whole number from 1 to 365.
 *
 * Kotlin notes: `toIntOrNull()` returns null instead of throwing for text like
 * "abc" or ""; `takeIf { ... }` keeps the value only if the condition holds
 * (null otherwise); `in 1..365` checks a range (both ends included).
 */
fun parseIntervalDays(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..365 }

/**
 * Dialog to enter a care task: type, a name (only for "Other") and the interval.
 * Stateless: every field's value comes in as a parameter and every change goes
 * out through a callback. `title` and `confirmText` are parameters so the same
 * dialog can later be used for editing.
 *
 * The confirm button is enabled only when the input is valid: an interval from
 * 1 to 365 and, for "Other", a non-blank name.
 */
@Composable
fun TaskDialog(
    title: String,
    confirmText: String,
    type: TaskType,
    onTypeChange: (TaskType) -> Unit,
    label: String,
    onLabelChange: (String) -> Unit,
    intervalText: String,
    onIntervalTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val intervalValid = parseIntervalDays(intervalText) != null
    val labelValid = type != TaskType.OTHER || label.isNotBlank()
    val canConfirm = intervalValid && labelValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // Column stacks its children vertically, 12 dp apart.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Segmented buttons: one choice out of a few, like radio buttons.
                // `TaskType.entries` is the list of all enum values, in order.
                SingleChoiceSegmentedButtonRow {
                    TaskType.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == type,
                            onClick = { onTypeChange(option) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = TaskType.entries.size),
                        ) {
                            Text(taskTypeName(option))
                        }
                    }
                }

                // Only "Other" tasks need a name; the field disappears otherwise.
                if (type == TaskType.OTHER) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = onLabelChange,
                        label = { Text(stringResource(R.string.task_label_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next, // moves to the next field
                        ),
                    )
                }

                OutlinedTextField(
                    value = intervalText,
                    onValueChange = onIntervalTextChange,
                    label = { Text(stringResource(R.string.task_interval_label)) },
                    singleLine = true,
                    // Number keyboard; it's still text, so it's validated above.
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm() }),
                    // Red outline and a hint while the text isn't a valid interval
                    // (but not for an empty field, to avoid scolding too early).
                    isError = intervalText.isNotEmpty() && !intervalValid,
                    supportingText = if (intervalText.isNotEmpty() && !intervalValid) {
                        { Text(stringResource(R.string.task_interval_error)) }
                    } else {
                        null
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = canConfirm) {
                Text(confirmText)
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
 * Confirmation before deleting a care task (its history goes too).
 * Same shape as [DeletePlantDialog]: stateless, Delete in the error color.
 */
@Composable
fun DeleteTaskDialog(
    taskName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_task_title, taskName)) },
        text = { Text(stringResource(R.string.delete_task_message)) },
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

@Preview
@Composable
private fun TaskDialogPreview() {
    PlantWatchTheme {
        TaskDialog(
            title = "Add care task", confirmText = "Add",
            type = TaskType.OTHER, onTypeChange = {},
            label = "Mist leaves", onLabelChange = {},
            intervalText = "3", onIntervalTextChange = {},
            onConfirm = {}, onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun DeleteTaskDialogPreview() {
    PlantWatchTheme {
        DeleteTaskDialog(taskName = "Water", onConfirm = {}, onDismiss = {})
    }
}
