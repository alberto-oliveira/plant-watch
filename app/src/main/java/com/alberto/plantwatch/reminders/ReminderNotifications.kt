package com.alberto.plantwatch.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.alberto.plantwatch.MainActivity
import com.alberto.plantwatch.R
import com.alberto.plantwatch.plants.data.DueTask
import com.alberto.plantwatch.plants.data.TaskType

/**
 * Everything about the reminder notification: its channel, building it, and
 * showing/removing it.
 *
 * Android notes:
 * - Since Android 8, every notification belongs to a *channel*. The user can
 *   tune each channel (sound, importance, on/off) in the app's system settings.
 *   Creating a channel that already exists does nothing, so it's safe to call
 *   on every app start.
 * - There is only ever one reminder notification: it always uses the same id,
 *   so posting again replaces the previous one instead of stacking a second.
 *
 * Kotlin note: `object` makes this a singleton with "static-like" functions;
 * `const val` is a compile-time constant (like Java's `static final`).
 */
object ReminderNotifications {

    private const val CHANNEL_ID = "reminders"
    const val NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT, // shows in the status bar and makes a sound
        ).apply {
            // `apply { }` runs the block with the object as `this` and returns
            // the object: a compact way to set extra properties.
            description = context.getString(R.string.reminder_channel_description)
        }
        // Looks up a platform service by its class.
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Shows (or replaces) the reminder for [due] tasks, e.g.
     *   3 care tasks due
     *   Water: Basil, Fern
     *   Fertilize: Pothos
     *   [Done] [Snooze 1 day]
     * Does nothing if notifications aren't allowed.
     */
    fun show(context: Context, due: List<DueTask>) {
        // Android 13+ requires the POST_NOTIFICATIONS permission. Checking it here,
        // right before posting, is also what Lint wants to see.
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val lines = groupDueTasks(due).map { group ->
            context.getString(
                R.string.reminder_line,
                taskName(context, group.type, group.customLabel),
                group.plantNames.joinToString(", "),
            )
        }
        val title = context.resources.getQuantityString(R.plurals.reminder_title, due.size, due.size)
        // The buttons act on exactly the tasks shown in this notification.
        val taskIds = due.map { it.taskId }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            // Collapsed view: everything on one line, separated by " / ".
            .setContentText(lines.joinToString(" / "))
            // Expanded view: one line per task kind.
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true) // tapping it opens the app and removes it
            // Action buttons (the icon argument is unused on current Android, hence 0).
            .addAction(
                0,
                context.getString(R.string.task_done),
                ReminderActionReceiver.pendingIntent(context, ReminderActionReceiver.ACTION_DONE, taskIds),
            )
            .addAction(
                0,
                context.getString(R.string.reminder_action_snooze),
                ReminderActionReceiver.pendingIntent(context, ReminderActionReceiver.ACTION_SNOOZE, taskIds),
            )
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Removes the reminder, if it's showing. */
    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /**
     * What happens when the notification is tapped: open the app.
     *
     * A PendingIntent is a "permission slip" letting the system (here, the
     * notification shade) start our activity later on our behalf.
     * FLAG_IMMUTABLE: nobody else can modify the intent inside it (required
     * since Android 12, and the safe default).
     */
    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            // Reuse the app if it's already open instead of stacking a second copy.
            // `or` is Kotlin's bitwise OR for combining flags (Java's `|`).
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * The task's display name, outside Compose (no stringResource here, so the
     * strings come from the Context). Mirrors taskName() in the UI.
     */
    private fun taskName(context: Context, type: TaskType, customLabel: String?): String = when (type) {
        TaskType.WATER -> context.getString(R.string.task_type_water)
        TaskType.FERTILIZE -> context.getString(R.string.task_type_fertilize)
        TaskType.OTHER -> customLabel ?: context.getString(R.string.task_type_other)
    }
}
