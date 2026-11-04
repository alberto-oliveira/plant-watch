package com.alberto.plantwatch.settings.ui

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alberto.plantwatch.R
import com.alberto.plantwatch.settings.Location
import com.alberto.plantwatch.settings.appDetailsIntent
import com.alberto.plantwatch.settings.ignoresBatteryOptimizations
import com.alberto.plantwatch.settings.notificationSettingsIntent
import com.alberto.plantwatch.settings.notificationsEnabled
import com.alberto.plantwatch.ui.IconBadge
import com.alberto.plantwatch.ui.theme.PlantWatchTheme
import com.alberto.plantwatch.weather.parseLocation
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
    val location by viewModel.location.collectAsStateWithLifecycle()
    val pauseFertilizing by viewModel.pauseFertilizingInWinter.collectAsStateWithLifecycle()
    val adjustWatering by viewModel.adjustWatering.collectAsStateWithLifecycle()
    val wateringFactor by viewModel.wateringFactor.collectAsStateWithLifecycle()
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    // Location dialog: open or not, and the two texts being typed.
    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
    var latitudeText by rememberSaveable { mutableStateOf("") }
    var longitudeText by rememberSaveable { mutableStateOf("") }

    // System statuses: owned by Android, not by the app, so they're read here
    // (not stored in the ViewModel) and refreshed every time the screen resumes,
    // e.g. when coming back from Android's settings after changing them.
    val context = LocalContext.current
    var notificationsOn by remember { mutableStateOf(notificationsEnabled(context)) }
    var batteryUnrestricted by remember { mutableStateOf(ignoresBatteryOptimizations(context)) }
    // `LifecycleResumeEffect`: the block runs on every ON_RESUME of the screen;
    // `onPauseOrDispose { }` is the required cleanup part (nothing to clean here).
    LifecycleResumeEffect(Unit) {
        notificationsOn = notificationsEnabled(context)
        batteryUnrestricted = ignoresBatteryOptimizations(context)
        onPauseOrDispose { }
    }

    SettingsScreen(
        reminderTime = reminderTime,
        location = location,
        pauseFertilizing = pauseFertilizing,
        onPauseFertilizingChange = viewModel::setPauseFertilizingInWinter,
        adjustWatering = adjustWatering,
        wateringFactor = wateringFactor,
        onAdjustWateringChange = viewModel::setAdjustWatering,
        notificationsOn = notificationsOn,
        batteryUnrestricted = batteryUnrestricted,
        onBack = onBack,
        onReminderTimeClick = { showTimePicker = true },
        onLocationClick = {
            // Prefill with the saved location (empty if none).
            latitudeText = location?.latitude?.let(::formatCoordinate) ?: ""
            longitudeText = location?.longitude?.let(::formatCoordinate) ?: ""
            showLocationDialog = true
        },
        // `startActivity` opens another app's screen (here, Android's Settings).
        onOpenNotificationSettings = { context.startActivity(notificationSettingsIntent(context)) },
        onOpenAppDetails = { context.startActivity(appDetailsIntent(context)) },
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

    if (showLocationDialog) {
        LocationDialog(
            latitude = latitudeText,
            onLatitudeChange = { latitudeText = it },
            longitude = longitudeText,
            onLongitudeChange = { longitudeText = it },
            onConfirm = {
                // Save is only enabled for valid input, so this is never null here.
                parseLocation(latitudeText, longitudeText)?.let { (lat, lon) ->
                    viewModel.setLocation(Location(lat, lon))
                }
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false },
        )
    }
}

/** The settings screen: a back arrow and the list of settings (stateless). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    reminderTime: LocalTime?,
    location: Location?,
    pauseFertilizing: Boolean,
    onPauseFertilizingChange: (Boolean) -> Unit,
    adjustWatering: Boolean,
    wateringFactor: Double,
    onAdjustWateringChange: (Boolean) -> Unit,
    notificationsOn: Boolean,
    batteryUnrestricted: Boolean,
    onBack: () -> Unit,
    onReminderTimeClick: () -> Unit,
    onLocationClick: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppDetails: () -> Unit,
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
                leadingContent = { SettingIcon(R.drawable.ic_schedule) },
                headlineContent = { Text(stringResource(R.string.settings_reminder_time)) },
                trailingContent = {
                    Text(
                        text = reminderTime?.let(::formatTime) ?: "",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
            )
            StatusRow(
                icon = R.drawable.ic_notifications,
                title = stringResource(R.string.settings_notifications),
                status = stringResource(
                    if (notificationsOn) R.string.settings_notifications_on else R.string.settings_notifications_off,
                ),
                isProblem = !notificationsOn,
                onOpenSettings = onOpenNotificationSettings,
            )
            StatusRow(
                icon = R.drawable.ic_battery_full,
                title = stringResource(R.string.settings_battery),
                status = stringResource(
                    if (batteryUnrestricted) R.string.settings_battery_unrestricted else R.string.settings_battery_optimized,
                ),
                isProblem = !batteryUnrestricted,
                onOpenSettings = onOpenAppDetails,
            )

            SectionTitle(stringResource(R.string.settings_section_care))
            ListItem(
                // Tapping the whole row toggles the switch, like Android's own settings.
                modifier = Modifier.clickable { onPauseFertilizingChange(!pauseFertilizing) },
                leadingContent = { SettingIcon(R.drawable.ic_compost) },
                headlineContent = { Text(stringResource(R.string.settings_pause_fertilizing)) },
                supportingContent = {
                    Text(
                        stringResource(
                            when {
                                location == null -> R.string.settings_pause_fertilizing_needs_location
                                location.latitude < 0 -> R.string.settings_pause_fertilizing_south
                                else -> R.string.settings_pause_fertilizing_north
                            },
                        ),
                    )
                },
                trailingContent = { Switch(checked = pauseFertilizing, onCheckedChange = onPauseFertilizingChange) },
            )
            ListItem(
                modifier = Modifier.clickable { onAdjustWateringChange(!adjustWatering) },
                leadingContent = { SettingIcon(R.drawable.ic_water_drop) },
                headlineContent = { Text(stringResource(R.string.settings_adjust_watering)) },
                supportingContent = {
                    // When on, also say what it does this week.
                    val percent = kotlin.math.abs(kotlin.math.round((wateringFactor - 1) * 100)).toInt()
                    val now = when {
                        percent == 0 -> stringResource(R.string.settings_adjust_watering_now_same)
                        wateringFactor > 1 -> stringResource(R.string.settings_adjust_watering_now_sooner, percent)
                        else -> stringResource(R.string.settings_adjust_watering_now_later, percent)
                    }
                    Text(
                        if (adjustWatering) "${stringResource(R.string.settings_adjust_watering_hint)}\n$now"
                        else stringResource(R.string.settings_adjust_watering_hint),
                    )
                },
                trailingContent = { Switch(checked = adjustWatering, onCheckedChange = onAdjustWateringChange) },
            )

            SectionTitle(stringResource(R.string.settings_section_sunlight))
            ListItem(
                modifier = Modifier.clickable(onClick = onLocationClick),
                leadingContent = { SettingIcon(R.drawable.ic_location_on) },
                headlineContent = { Text(stringResource(R.string.settings_location)) },
                supportingContent = {
                    Text(
                        // Location is a data class, so `{ (lat, lon) -> }` destructures
                        // it into its two properties, in declaration order.
                        text = location?.let { (lat, lon) -> "${formatCoordinate(lat)}, ${formatCoordinate(lon)}" }
                            ?: stringResource(R.string.settings_location_not_set),
                    )
                },
            )
        }
    }
}

/**
 * A system setting's status with a shortcut to Android's settings. Problems are
 * shown in the error color so they stand out.
 */
@Composable
private fun StatusRow(
    @DrawableRes icon: Int,
    title: String,
    status: String,
    isProblem: Boolean,
    onOpenSettings: () -> Unit,
) {
    ListItem(
        leadingContent = { SettingIcon(icon) },
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                text = status,
                color = if (isProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.settings_open_system))
            }
        },
    )
}

/** The small round icon at the start of each settings row. */
@Composable
private fun SettingIcon(@DrawableRes icon: Int) {
    IconBadge(
        icon = icon,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        size = 36.dp,
    )
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
        SettingsScreen(
            reminderTime = LocalTime.of(9, 0), location = Location(-23.5505, -46.6333), pauseFertilizing = true, onPauseFertilizingChange = {},
            adjustWatering = true, wateringFactor = 1.07, onAdjustWateringChange = {},
            notificationsOn = true, batteryUnrestricted = true,
            onBack = {}, onReminderTimeClick = {}, onLocationClick = {},
            onOpenNotificationSettings = {}, onOpenAppDetails = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenProblemsPreview() {
    PlantWatchTheme {
        SettingsScreen(
            reminderTime = LocalTime.of(9, 0), location = null, pauseFertilizing = false, onPauseFertilizingChange = {},
            adjustWatering = false, wateringFactor = 1.0, onAdjustWateringChange = {},
            notificationsOn = false, batteryUnrestricted = false,
            onBack = {}, onReminderTimeClick = {}, onLocationClick = {},
            onOpenNotificationSettings = {}, onOpenAppDetails = {},
        )
    }
}
