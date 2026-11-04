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
 * Everything about the reminder notifications: their channel, building them,
 * and showing/removing them.
 *
 * There is one notification **per plant** with due tasks ("Basil: Water,
 * Fertilize", with Done and Snooze for that plant), bundled under a group
 * *summary* ("3 care tasks due"). Android shows the group as one expandable
 * entry; on screens without groups the summary stands in for all of them.
 *
 * Android notes:
 * - Since Android 8, every notification belongs to a *channel*. The user can
 *   tune each channel (sound, importance, on/off) in the app's system settings.
 *   Creating a channel that already exists does nothing, so it's safe to call
 *   on every app start.
 * - A notification is identified by its id: posting with the same id replaces
 *   it. The summary always uses [SUMMARY_ID]; each plant uses
 *   [PLANT_ID_BASE] + its plant id, so plants never share a notification.
 *
 * Kotlin note: `object` makes this a singleton with "static-like" functions;
 * `const val` is a compile-time constant (like Java's `static final`).
 */
object ReminderNotifications {

    const val CHANNEL_ID = "reminders"
    private const val SUMMARY_ID = 1
    private const val PLANT_ID_BASE = 1000
    private const val GROUP_KEY = "com.alberto.plantwatch.REMINDERS"

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
     * Shows the reminders for [due] tasks, replacing earlier ones: one
     * notification per plant plus the group summary, e.g.
     *   Basil              Water, Fertilize      [Done] [Snooze 1 day]
     *   Fern               Water                 [Done] [Snooze 1 day]
     * Plants that are no longer due lose their notification.
     *
     * [silent]: update without sound or vibration (used after a button press,
     * when the user is already looking at them). Otherwise only the summary
     * alerts, so a new day's reminder makes one sound, not one per plant.
     *
     * Does nothing if notifications aren't allowed.
     */
    fun show(context: Context, due: List<DueTask>, silent: Boolean = false) {
        // Android 13+ requires the POST_NOTIFICATIONS permission. Checking it here,
        // right before posting, is also what Lint wants to see.
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val manager = NotificationManagerCompat.from(context)
        val plants = groupDueByPlant(due)

        // Remove notifications of plants that are no longer due.
        val shownIds = plants.map { PLANT_ID_BASE + it.plantId.toInt() }.toSet()
        manager.activeNotifications
            .filter { it.id >= PLANT_ID_BASE && it.id !in shownIds }
            .forEach { manager.cancel(it.id) }

        for (plant in plants) {
            val id = PLANT_ID_BASE + plant.plantId.toInt()
            val taskIds = plant.tasks.map { it.taskId }
            val notification = baseBuilder(context, silent)
                .setContentTitle(plant.plantName)
                .setContentText(plant.tasks.joinToString(", ") { taskName(context, it.type, it.customLabel) })
                // The buttons act on this plant's due tasks. The notification id
                // doubles as the PendingIntents' request code, keeping each
                // plant's buttons separate from the others'.
                .addAction(
                    0,
                    context.getString(R.string.task_done),
                    ReminderActionReceiver.pendingIntent(context, ReminderActionReceiver.ACTION_DONE, taskIds, id),
                )
                .addAction(
                    0,
                    context.getString(R.string.reminder_action_snooze),
                    ReminderActionReceiver.pendingIntent(context, ReminderActionReceiver.ACTION_SNOOZE, taskIds, id),
                )
                .build()
            manager.notify(id, notification)
        }

        // The summary: what's due by kind of task, e.g. "Water: Basil, Fern".
        val lines = groupDueTasks(due).map { group ->
            context.getString(
                R.string.reminder_line,
                taskName(context, group.type, group.customLabel),
                group.plantNames.joinToString(", "),
            )
        }
        val summary = baseBuilder(context, silent)
            .setContentTitle(context.resources.getQuantityString(R.plurals.reminder_title, due.size, due.size))
            .setContentText(lines.joinToString(" / "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setGroupSummary(true)
            .build()
        manager.notify(SUMMARY_ID, summary)
    }

    /** What every reminder notification has in common. */
    private fun baseBuilder(context: Context, silent: Boolean) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true) // tapping it opens the app and removes it
            .setGroup(GROUP_KEY)
            // Within the group, only the summary plays the sound.
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setSilent(silent)

    /** Removes all reminders (the summary and every plant's). */
    fun cancel(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.activeNotifications
            .filter { it.id == SUMMARY_ID || it.id >= PLANT_ID_BASE }
            .forEach { manager.cancel(it.id) }
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
