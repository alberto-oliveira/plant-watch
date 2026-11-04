package com.alberto.plantwatch.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.alberto.plantwatch.reminders.ReminderNotifications

/*
 * Read-only checks of system settings that affect reminders, and intents that
 * open the matching pages in Android's Settings app. These values belong to the
 * system (the user can change them anytime outside the app), so they're read
 * fresh whenever the settings screen is shown, never stored.
 */

/**
 * True if the reminder can actually appear: notifications allowed for the app
 * (this covers the POST_NOTIFICATIONS permission and the app-wide toggle) and the
 * "Care reminders" channel not switched off.
 */
fun notificationsEnabled(context: Context): Boolean {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
    val channel = context.getSystemService(NotificationManager::class.java)
        .getNotificationChannel(ReminderNotifications.CHANNEL_ID)
    // A channel that doesn't exist yet will be created with the default importance.
    return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
}

/**
 * True if Android exempts the app from battery optimization ("Unrestricted"), so
 * background work such as the daily reminder isn't postponed to save battery.
 */
fun ignoresBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

/** Opens the app's notification settings page. */
fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

/**
 * Opens the app's "App info" page, where Battery can be set to Unrestricted.
 * (A dialog asking for the exemption directly would need an extra permission,
 * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS; one more tap isn't worth it.)
 * `package:<name>` is the URI form Android uses to say which app.
 */
fun appDetailsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
