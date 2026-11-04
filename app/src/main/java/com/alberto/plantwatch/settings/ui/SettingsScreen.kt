package com.alberto.plantwatch.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alberto.plantwatch.R
import com.alberto.plantwatch.ui.theme.PlantWatchTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Connects [SettingsViewModel] to the stateless [SettingsScreen] and the time picker. */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val reminderTime by viewModel.reminderTime.collectAsStateWithLifecycle()
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    SettingsScreen(
        reminderTime = reminderTime,
        onBack = onBack,
        onReminderTimeClick = { showTimePicker = true },
    )

    // `val time = reminderTime` copies the property into a local value, so the
    // compiler can smart-cast it to non-null inside the `if`.
    val time = reminderTime
    if (showTimePicker && time != null) {
        ReminderTimeDialog(
            initialTime = time,
            onConfirm = { picked ->
                viewModel.setReminderTime(picked)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

/** The settings screen: a back arrow and the list of settings (stateless). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    reminderTime: LocalTime?,
    onBack: () -> Unit,
    onReminderTimeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        // A plain scrollable Column: few, fixed items, so no LazyColumn needed.
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(stringResource(R.string.settings_section_reminders))
            ListItem(
                modifier = Modifier.clickable(onClick = onReminderTimeClick),
                headlineContent = { Text(stringResource(R.string.settings_reminder_time)) },
                trailingContent = {
                    Text(
                        text = reminderTime?.let(::formatTime) ?: "",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/**
 * A dialog with Material 3's clock-style time picker (24-hour).
 * `rememberTimePickerState` keeps the picked hour/minute, including across
 * rotation; the time is only applied when Save is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reminder_time_dialog_title)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
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
 * A time in the phone's format ("09:00"). `::formatTime` above is a function
 * reference passed to `let`, short for `{ formatTime(it) }`.
 */
private fun formatTime(time: LocalTime): String =
    time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PlantWatchTheme {
        SettingsScreen(reminderTime = LocalTime.of(9, 0), onBack = {}, onReminderTimeClick = {})
    }
}
