package com.alberto.plantwatch.plants.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alberto.plantwatch.R
import com.alberto.plantwatch.plants.data.HomeRoom
import com.alberto.plantwatch.ui.theme.PlantWatchTheme
import com.alberto.plantwatch.weather.Compass

/*
 * Rooms: picking a plant's room, and editing a room (in Settings).
 */

/**
 * Picks the plant's room: one of the existing rooms, "No room", or a new room
 * typed in the field below the list (created and selected in one go).
 */
@Composable
fun RoomPickerDialog(
    rooms: List<HomeRoom>,
    currentRoomId: Long?,
    newRoomName: String,
    onNewRoomNameChange: (String) -> Unit,
    onPick: (Long?) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.room_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(rooms, key = { it.id }) { room ->
                        RoomOption(room.name, selected = room.id == currentRoomId) { onPick(room.id) }
                    }
                    item(key = "none") {
                        RoomOption(stringResource(R.string.room_none), selected = currentRoomId == null) { onPick(null) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = onNewRoomNameChange,
                        label = { Text(stringResource(R.string.room_new)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onCreate, enabled = newRoomName.isNotBlank()) {
                        Text(stringResource(R.string.add_plant_confirm))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** One choice in the room picker: a radio button and the room's name; the whole row is tappable. */
@Composable
private fun RoomOption(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Adds or edits a room in Settings: its name and, optionally, the direction its
 * window faces (offered as the default for window-based sun tracking).
 * [onDelete] is null when adding a new room.
 */
@Composable
fun RoomEditDialog(
    isNew: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    windowDirection: Compass?,
    onWindowDirectionChange: (Compass?) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.room_add_title else R.string.room_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.room_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.room_window_label), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // The "unknown" choice first, then the compass directions.
                    FilterChip(
                        selected = windowDirection == null,
                        onClick = { onWindowDirectionChange(null) },
                        label = { Text(stringResource(R.string.room_window_unknown)) },
                    )
                    Compass.entries.forEach { direction ->
                        FilterChip(
                            selected = direction == windowDirection,
                            onClick = { onWindowDirectionChange(direction) },
                            label = { Text(compassName(direction)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete_plant_confirm)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Preview
@Composable
private fun RoomPickerDialogPreview() {
    PlantWatchTheme {
        RoomPickerDialog(
            rooms = listOf(HomeRoom(1, "Sala", Compass.N), HomeRoom(2, "Varanda")),
            currentRoomId = 1, newRoomName = "", onNewRoomNameChange = {},
            onPick = {}, onCreate = {}, onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun RoomEditDialogPreview() {
    PlantWatchTheme {
        RoomEditDialog(
            isNew = false, name = "Sala", onNameChange = {},
            windowDirection = Compass.N, onWindowDirectionChange = {},
            onSave = {}, onDelete = {}, onDismiss = {},
        )
    }
}
