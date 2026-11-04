package com.alberto.plantwatch.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alberto.plantwatch.PlantWatchApplication
import com.alberto.plantwatch.plants.data.CareLog
import com.alberto.plantwatch.plants.markedDone
import com.alberto.plantwatch.plants.snoozed
import com.alberto.plantwatch.weather.intervalWhenDone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Handles the reminder notification's **Done** and **Snooze 1 day** buttons,
 * updating Room directly without opening the app. Both act on every task listed
 * in the notification.
 *
 * Android notes:
 * - A BroadcastReceiver is a small entry point the system calls with an Intent.
 *   This one is registered in the manifest with `exported="false"`: only our own
 *   app (through the notification's PendingIntents) can trigger it, so no other
 *   app can mark tasks done.
 * - `onReceive` runs on the main thread and must return quickly. Database work
 *   can't run there, so `goAsync()` tells the system "I'm not finished yet"; the
 *   work then runs in a background coroutine and calls `finish()` when done (the
 *   system allows about 10 seconds, plenty for a few updates).
 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // `?: return`: if the extra is missing, there's nothing to do.
        val taskIds = intent.getLongArrayExtra(EXTRA_TASK_IDS) ?: return
        val action = intent.action
        val pendingResult = goAsync()
        val app = context.applicationContext as PlantWatchApplication

        // Dispatchers.IO: a thread pool meant for disk and network work.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = app.container.database.careTaskDao()
                val today = LocalDate.now()
                // Read the tasks fresh and keep only those still due: if a task was
                // already done in the app (or deleted), the button doesn't touch it.
                // `!isAfter(today)` means "nextDue <= today".
                val stillDue = dao.getByIds(taskIds.toList()).filter { !it.nextDue.isAfter(today) }

                // Same weather adjustment for watering as when marking done in the app.
                val settings = app.container.settingsStore
                val factor = if (settings.adjustWateringToWeather.first()) settings.wateringFactor.first() else null

                when (action) {
                    ACTION_DONE -> stillDue.forEach { task ->
                        dao.markDone(
                            markedDone(task, today, intervalWhenDone(task, factor)),
                            CareLog(taskId = task.id, doneOn = today),
                        )
                    }
                    ACTION_SNOOZE -> stillDue.forEach { task ->
                        dao.update(snoozed(task, today))
                    }
                }
                ReminderNotifications.cancel(context)
            } finally {
                // `finally` runs even if something above throws, so the system is
                // always told we're done.
                pendingResult.finish()
            }
        }
    }

    companion object {
        // Action names: any unique string; prefixing with the package avoids clashes.
        const val ACTION_DONE = "com.alberto.plantwatch.action.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.alberto.plantwatch.action.REMINDER_SNOOZE"
        private const val EXTRA_TASK_IDS = "taskIds"

        /**
         * The PendingIntent for one of the buttons: an *explicit* intent (it names
         * this class, so it can only reach this receiver) carrying the task ids.
         *
         * - FLAG_IMMUTABLE: the notification system can't alter the intent.
         * - FLAG_UPDATE_CURRENT: a new reminder replaces the extras (the task ids)
         *   of the previous one instead of reusing stale ones.
         * - Done and Snooze have different actions, so Android treats them as two
         *   separate PendingIntents.
         */
        fun pendingIntent(context: Context, action: String, taskIds: List<Long>): PendingIntent {
            val intent = Intent(context, ReminderActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_TASK_IDS, taskIds.toLongArray())
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
