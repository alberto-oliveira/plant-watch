package com.alberto.plantwatch.settings.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alberto.plantwatch.R
import com.alberto.plantwatch.backup.BackupContents
import com.alberto.plantwatch.backup.BackupData
import com.alberto.plantwatch.backup.BackupSettings
import com.alberto.plantwatch.ui.theme.PlantWatchTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The dialog for an export or import in progress or finished: a spinner while
 * working, the confirmation before an import replaces everything, or the result.
 */
@Composable
fun BackupStatusDialog(
    status: BackupStatus,
    onConfirmImport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val confirming = status is BackupStatus.ReadyToImport
    AlertDialog(
        // While working, the dialog can't be dismissed (nothing to cancel).
        onDismissRequest = { if (status != BackupStatus.Working) onDismiss() },
        title = if (confirming) {
            { Text(stringResource(R.string.backup_import_title)) }
        } else {
            null
        },
        text = {
            // `when` over a sealed interface: the compiler checks every case is handled.
            when (status) {
                BackupStatus.Working -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.backup_working))
                }
                BackupStatus.Exported -> Text(stringResource(R.string.backup_exported))
                BackupStatus.Imported -> Text(stringResource(R.string.backup_imported))
                BackupStatus.Refused -> Text(stringResource(R.string.backup_refused))
                BackupStatus.Failed -> Text(stringResource(R.string.backup_failed))
                is BackupStatus.ReadyToImport -> Text(importSummary(status.contents))
            }
        },
        confirmButton = {
            when {
                confirming -> TextButton(onClick = onConfirmImport) { Text(stringResource(R.string.backup_import_confirm)) }
                status != BackupStatus.Working -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
            }
        },
        dismissButton = if (confirming) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
        } else {
            null
        },
    )
}

/** "Everything … replaced with this backup from 5 Nov 2026: 3 plants, 7 tasks, …". */
@Composable
private fun importSummary(contents: BackupContents): String {
    val data = contents.data
    val photoCount = data.plants.count { it.photoFile != null && it.photoFile in contents.photos }
    val parts = listOf(
        pluralStringResource(R.plurals.backup_plants, data.plants.size, data.plants.size),
        pluralStringResource(R.plurals.backup_tasks, data.tasks.size, data.tasks.size),
        pluralStringResource(R.plurals.backup_logs, data.logs.size, data.logs.size),
        pluralStringResource(R.plurals.backup_photos, photoCount, photoCount),
    )
    // exportedAt is "2026-11-05T10:00:00"; its first 10 characters are the date.
    val date = runCatching {
        LocalDate.parse(data.exportedAt.take(10)).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }.getOrDefault(data.exportedAt)
    return stringResource(R.string.backup_import_message, date, parts.joinToString(", "))
}

@Preview
@Composable
private fun BackupConfirmPreview() {
    PlantWatchTheme {
        BackupStatusDialog(
            status = BackupStatus.ReadyToImport(
                BackupContents(
                    BackupData(schemaVersion = 6, exportedAt = "2026-11-05T10:00:00", settings = BackupSettings(540)),
                    emptyMap(),
                ),
            ),
            onConfirmImport = {}, onDismiss = {},
        )
    }
}
