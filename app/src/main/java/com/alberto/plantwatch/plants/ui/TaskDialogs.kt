package com.alberto.plantwatch.plants.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.alberto.plantwatch.R
import com.alberto.plantwatch.ui.theme.PlantWatchTheme

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
private fun DeleteTaskDialogPreview() {
    PlantWatchTheme {
        DeleteTaskDialog(taskName = "Water", onConfirm = {}, onDismiss = {})
    }
}
