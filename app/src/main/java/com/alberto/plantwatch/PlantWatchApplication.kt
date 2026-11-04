package com.alberto.plantwatch

import android.app.Application
import com.alberto.plantwatch.reminders.ReminderNotifications
import com.alberto.plantwatch.reminders.scheduleDailyReminder

/**
 * Custom Application class: Android creates exactly one instance when the app
 * process starts, before any activity, receiver or worker. That makes it the
 * natural owner of app-wide objects such as the [AppContainer].
 *
 * It's registered in AndroidManifest.xml via `android:name=".PlantWatchApplication"`.
 *
 * Kotlin notes:
 * - `lateinit var` declares a non-null property that is assigned later (here,
 *   in onCreate) instead of in the constructor. Reading it before assignment
 *   throws an exception rather than returning null.
 * - `private set`: anyone can read `container`, only this class can assign it.
 * - Other code reaches it with `(context.applicationContext as PlantWatchApplication).container`
 *   (`as` is a cast).
 */
class PlantWatchApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // `this` is the Application, a Context that lives as long as the app,
        // so the database never holds on to a short-lived Activity.
        container = AppContainer(this)
        // The reminder notification's channel must exist before anything posts to it.
        ReminderNotifications.createChannel(this)
        // Schedule the daily reminder job (does nothing if it's already scheduled).
        scheduleDailyReminder(this)
    }
}
