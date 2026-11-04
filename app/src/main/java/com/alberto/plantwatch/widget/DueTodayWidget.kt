package com.alberto.plantwatch.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.alberto.plantwatch.MainActivity
import com.alberto.plantwatch.PlantWatchApplication
import com.alberto.plantwatch.R
import com.alberto.plantwatch.reminders.PlantDue
import com.alberto.plantwatch.reminders.ReminderNotifications
import com.alberto.plantwatch.reminders.groupDueByPlant
import com.alberto.plantwatch.reminders.observeReminderTasks
import com.alberto.plantwatch.ui.theme.DarkColors
import com.alberto.plantwatch.ui.theme.LightColors
import java.time.LocalDate

/**
 * The "Due today" home-screen widget: the plants that need care today (the
 * same list as the daily reminder), or "All done". Tapping it opens the app.
 *
 * How Glance works: a widget isn't a live view inside the home-screen app, so
 * Glance runs these Compose-style functions in our process and turns the
 * result into RemoteViews, a description of the layout that the launcher
 * draws. Only Glance's own building blocks can be used (`androidx.glance.*`:
 * Column, Text, GlanceModifier...), not regular Compose UI or Material 3.
 *
 * While Glance keeps the widget's session open it follows the database
 * (the list is a Flow); after that, [refreshWidget] redraws it.
 */
class DueTodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as PlantWatchApplication
        val dueTasks = observeReminderTasks(app, LocalDate.now())
        provideContent {
            // null until the first database read.
            val due by dueTasks.collectAsState(initial = null)
            GlanceTheme(colors = WidgetColors) {
                DueTodayContent(due?.let(::groupDueByPlant))
            }
        }
    }

    companion object {
        /** At most this many plants are listed; the rest become "+N more". */
        const val MAX_PLANTS = 6
    }
}

/** The app's own light and dark colours, for the widget. */
private val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

@Composable
private fun DueTodayContent(plants: List<PlantDue>?) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            text = context.getString(R.string.widget_title),
            style = TextStyle(color = colors.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.height(6.dp))
        when {
            plants == null -> Unit
            plants.isEmpty() -> Text(
                text = context.getString(R.string.widget_all_done),
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = 14.sp),
            )
            else -> {
                plants.take(DueTodayWidget.MAX_PLANTS).forEach { plant ->
                    Row(modifier = GlanceModifier.padding(vertical = 2.dp)) {
                        Text(
                            text = plant.plantName,
                            style = TextStyle(color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            maxLines = 1,
                        )
                        Text(
                            text = " · " + plant.tasks.joinToString(", ") {
                                ReminderNotifications.taskName(context, it.type, it.customLabel)
                            },
                            style = TextStyle(color = colors.onSurfaceVariant, fontSize = 14.sp),
                            maxLines = 1,
                        )
                    }
                }
                val more = plants.size - DueTodayWidget.MAX_PLANTS
                if (more > 0) {
                    Text(
                        text = context.resources.getQuantityString(R.plurals.widget_more, more, more),
                        style = TextStyle(color = colors.onSurfaceVariant, fontSize = 13.sp),
                    )
                }
            }
        }
    }
}

/**
 * Connects the widget to Android: the system talks to widgets through a
 * BroadcastReceiver (registered in the manifest with the widget's info XML).
 */
class DueTodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DueTodayWidget()
}

/**
 * Redraws every placed widget. Called after the daily reminder, after the
 * notification buttons, after an import, and when the app goes to the
 * background. Without placed widgets it does nothing.
 */
suspend fun refreshWidget(context: Context) {
    DueTodayWidget().updateAll(context)
}
