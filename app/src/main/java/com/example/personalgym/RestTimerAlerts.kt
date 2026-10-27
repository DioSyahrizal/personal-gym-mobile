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
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** True while an activity of the app is visible; the in-app timer handles alerts then. */
object AppVisibility {
    @Volatile
    var isForeground: Boolean = false
}

/**
 * Keeps the rest timer working when the phone is locked or the app is in the background:
 * a silent lock-screen notification counts down, and an exact alarm fires a vibrating
 * "time's up" notification when it ends.
 */
object RestTimerAlerts {
    private const val CHANNEL_COUNTDOWN = "rest_countdown"
    private const val CHANNEL_DONE = "rest_done"
    private const val ID_COUNTDOWN = 1001
    private const val ID_DONE = 1002
    private const val EXTRA_LABEL = "label"
    private val VIBRATION = longArrayOf(0, 400, 200, 400, 200, 600)

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_COUNTDOWN, "Rest timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the running rest timer on the lock screen"
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Rest finished", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Vibrates when it's time for your next set"
                enableVibration(true)
                vibrationPattern = VIBRATION
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build(),
                )
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            },
        )
    }

    fun schedule(context: Context, c: Countdown) {
        showCountdown(context, c)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context, finishedText(c))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.endsAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.endsAt, pi)
        }
    }

    /** Stops the alarm and clears both notifications. */
    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context, ""))
        NotificationManagerCompat.from(context).apply {
            cancel(ID_COUNTDOWN)
            cancel(ID_DONE)
        }
    }

    fun vibrate(context: Context) {
        val v = context.getSystemService(Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(VIBRATION, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(VIBRATION, -1)
        }
    }

    private fun finishedText(c: Countdown) =
        if (c.onFinish is FinishAction.None) "Rest's over — time for your next set 💪" else "${c.label} done! Rest now."

    private fun showCountdown(context: Context, c: Countdown) {
        val n = NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
            .setSmallIcon(R.drawable.ic_stat_dumbbell)
            .setContentTitle(if (c.onFinish is FinishAction.None) "Resting" else c.label)
            .setContentText("Next set when the timer ends")
            .setWhen(c.endsAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setTimeoutAfter((c.endsAt - System.currentTimeMillis()).coerceAtLeast(0))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(context))
            .build()
        post(context, ID_COUNTDOWN, n)
    }

    internal fun showFinished(context: Context, text: String) {
        val n = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_dumbbell)
            .setContentTitle("Time's up!")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(VIBRATION)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        NotificationManagerCompat.from(context).cancel(ID_COUNTDOWN)
        post(context, ID_DONE, n)
    }

    private fun post(context: Context, id: Int, n: android.app.Notification) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    private fun alarmIntent(context: Context, text: String): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, RestAlarmReceiver::class.java).putExtra(EXTRA_LABEL, text),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    internal fun labelFrom(intent: Intent) = intent.getStringExtra(EXTRA_LABEL).orEmpty()
}

class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // In the foreground the in-app timer beeps and vibrates instead.
        if (AppVisibility.isForeground) return
        // The "rest finished" channel vibrates; apps can't vibrate directly from the background.
        RestTimerAlerts.showFinished(context, RestTimerAlerts.labelFrom(intent))
    }
}
