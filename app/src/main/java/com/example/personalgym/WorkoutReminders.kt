package com.example.personalgym

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.personalgym.data.PlanDay
import com.example.personalgym.data.Plans
import com.example.personalgym.data.Stats
import com.example.personalgym.data.Storage
import java.util.Calendar

/**
 * Daily "time to train" notification at the user's chosen time. The alarm fires every day;
 * whether to notify is decided at fire time from the current plan, so rest days, days you
 * already trained, and plan changes are all handled without rescheduling.
 */
object WorkoutReminders {
    private const val CHANNEL = "workout_reminder"
    private const val NOTIFICATION_ID = 2001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Workout reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A nudge on training days at the time you choose"
            },
        )
    }

    /** Sets (or clears) the next alarm from the saved settings. Safe to call any time. */
    fun schedule(context: Context) {
        val storage = Storage(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context)
        am.cancel(pi)
        if (!storage.reminderEnabled) return
        val at = nextTrigger(storage.reminderMinutes, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    /** Next occurrence of [minutesOfDay] strictly after [now]. */
    fun nextTrigger(minutesOfDay: Int, now: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= now) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    /** Today's plan day if it is a training day you haven't done yet, otherwise null. */
    internal fun dayToRemind(context: Context): PlanDay? {
        val storage = Storage(context)
        if (storage.loadActive() != null) return null // already mid-workout
        val profile = storage.profile ?: Plans.profileFor(storage.activePlanId)
        val edits = storage.loadPlanEdits()[Plans.base(profile.goal, profile.daysPerWeek).id].orEmpty()
        val today = Stats.dayOfWeek()
        val day = Plans.forProfile(profile, edits).days.first { it.dayOfWeek == today }
        if (day.isRest) return null
        if (today in Stats.trainedDaysThisWeek(storage.loadSessions())) return null
        return day
    }

    internal fun onAlarm(context: Context) {
        dayToRemind(context)?.let { show(context, it) }
        schedule(context) // tomorrow
    }

    private fun show(context: Context, day: PlanDay) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val open = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_dumbbell)
            .setContentTitle("${day.title} today 💪")
            .setContentText("${day.exercises.size} exercises · ${day.focus}")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n) }
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, WorkoutReminderReceiver::class.java).setAction(ACTION_FIRE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    internal const val ACTION_FIRE = "com.example.personalgym.REMINDER"
}

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WorkoutReminders.ACTION_FIRE -> WorkoutReminders.onAlarm(context)
            // Alarms are cleared on reboot and app update; put the next one back.
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> WorkoutReminders.schedule(context)
        }
    }
}
