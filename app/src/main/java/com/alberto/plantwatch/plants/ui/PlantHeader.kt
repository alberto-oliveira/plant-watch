package com.alberto.plantwatch.plants.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.alberto.plantwatch.R
import com.alberto.plantwatch.ui.theme.PlantWatchTheme
import java.io.File

/**
 * The top of the plant screen: the plant's photo (if any) with buttons to add,
 * change or remove it, and its notes (tap to edit).
 *
 * Coil's `AsyncImage` loads the file in the background and shows it when ready,
 * scaled to fill the width (`ContentScale.Crop` cuts off what doesn't fit).
 */
@Composable
fun PlantHeaderCard(
    photo: File?,
    notes: String?,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onEditNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = stringResource(R.string.plant_photo_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPickPhoto) {
                    Text(stringResource(if (photo == null) R.string.plant_photo_add else R.string.plant_photo_change))
                }
                if (photo != null) {
                    TextButton(onClick = onRemovePhoto) { Text(stringResource(R.string.plant_photo_remove)) }
                }
            }
            // Notes, or an invitation to add some; tapping edits them.
            Text(
                text = notes ?: stringResource(R.string.plant_notes_empty),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = if (notes == null) FontStyle.Italic else FontStyle.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditNotes)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

/** Dialog to edit the plant's notes (several lines; empty removes them). */
@Composable
fun NotesDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plant_notes_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                // Several lines, growing up to a limit, then scrolling.
                minLines = 3,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun PlantHeaderCardPreview() {
    PlantWatchTheme {
        PlantHeaderCard(
            photo = null, notes = "Gift from Ana. Repotted in March.",
            onPickPhoto = {}, onRemovePhoto = {}, onEditNotes = {},
        )
    }
}

@Preview
@Composable
private fun NotesDialogPreview() {
    PlantWatchTheme {
        NotesDialog(text = "Gift from Ana.", onTextChange = {}, onConfirm = {}, onDismiss = {})
    }
}
