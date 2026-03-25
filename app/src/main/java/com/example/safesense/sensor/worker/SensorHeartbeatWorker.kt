package com.example.safesense.sensor.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.safesense.MainActivity
import com.example.safesense.data.preferences.UserPreferences
import com.example.safesense.sensor.SensorForegroundService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

// ─────────────────────────────────────────────────────────────────────────────
// SensorHeartbeatWorker.kt
// Location: sensor/worker/SensorHeartbeatWorker.kt
//
// WHY THIS EXISTS:
//   Tecno (HiOS) and Infinix (XOS) aggressively kill background processes
//   to save battery. They will freeze our Foreground Service silently.
//   The user sees the persistent notification and thinks they are protected.
//   They are not. This is the most dangerous failure mode in the entire app.
//
// HOW IT WORKS:
//   1. AccelerometerProcessor writes System.currentTimeMillis() to DataStore
//      every time it processes an accelerometer reading.
//      (This write is done in SensorForegroundService.collectAccelerometerEvents)
//
//   2. This worker runs every 3 minutes via WorkManager PeriodicWorkRequest.
//      WorkManager is battery-aware and survives most aggressive OEM killers
//      better than a Foreground Service alone.
//
//   3. On each run, the worker reads LAST_ACCELEROMETER_TIMESTAMP from DataStore.
//      If that timestamp is more than 5 minutes old (or was never written),
//      it means AccelerometerProcessor has not run in over 5 minutes —
//      the Foreground Service has been silently killed.
//
//   4. The worker fires a local notification:
//      "SafeSense may have been paused by your phone. Tap to restore monitoring."
//
//   5. Tapping the notification sends ACTION_START to SensorForegroundService,
//      which restarts the sensor engine.
//
// WHY 5 MINUTES AS THE STALENESS THRESHOLD?
//   The worker runs every 3 minutes. If it runs and finds a 3-minute-old
//   timestamp, everything is fine. 5 minutes gives one full worker period
//   of grace before alarming — this avoids false notifications from WorkManager
//   scheduling jitter (WorkManager does not guarantee exact timing).
//
// WHY CoroutineWorker NOT Worker?
//   DataStore.data returns a Flow. We need to read from it with .first(),
//   which is a suspending function. CoroutineWorker provides a coroutine
//   context for this. Regular Worker would require runBlocking(), which
//   blocks a thread — bad practice.
//
// WHY @HiltWorker NOT @AndroidEntryPoint?
//   Workers cannot use @AndroidEntryPoint. Hilt provides @HiltWorker + @AssistedInject
//   specifically for WorkManager. Without this, Hilt cannot inject DataStore here.
//   The HiltWorkerFactory must be set on WorkManager — this is done in
//   SafeSenseApp.kt (the Application class).
//
// NOTIFICATION CHANNEL:
//   We create a SEPARATE notification channel for heartbeat alerts
//   (HEARTBEAT_CHANNEL_ID = "safesense_heartbeat_channel").
//   This is NOT the same as the persistent monitoring notification channel.
//   Reason: Users should be able to silence the persistent channel (low priority)
//   while keeping heartbeat alerts audible (default priority = shows in drawer).
// ─────────────────────────────────────────────────────────────────────────────

@HiltWorker
class SensorHeartbeatWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataStore: DataStore<Preferences>
) : CoroutineWorker(context, workerParams) {

    companion object {
        // Used by SensorForegroundService to identify this periodic job
        // so it can cancel it when monitoring stops.
        const val WORK_NAME = "safesense_heartbeat"

        // Notification IDs and channel
        const val HEARTBEAT_CHANNEL_ID    = "safesense_heartbeat_channel"
        const val HEARTBEAT_NOTIFICATION_ID = 2  // must be different from the service's ID (1)

        // 5 minutes in milliseconds
        // Timestamp older than this = service is dead
        const val STALENESS_THRESHOLD_MS = 5 * 60 * 1000L
    }

    override suspend fun doWork(): Result {
        // ── Step 1: Read the last accelerometer timestamp from DataStore ──────

        val prefs = dataStore.data.first()

        // Check if monitoring is even supposed to be running.
        // If the user deliberately stopped monitoring, do not alert them.
        val isMonitoring = prefs[UserPreferences.IS_MONITORING] ?: false
        if (!isMonitoring) {
            android.util.Log.d("SafeSense/Heartbeat", "Monitoring is off — skipping check")
            return Result.success()
        }

        val lastTimestamp = prefs[UserPreferences.LAST_ACCELEROMETER_TIMESTAMP] ?: 0L
        val now           = System.currentTimeMillis()
        val age           = now - lastTimestamp

        android.util.Log.d(
            "SafeSense/Heartbeat",
            "Last accelerometer reading: ${age / 1000}s ago (threshold: ${STALENESS_THRESHOLD_MS / 1000}s)"
        )

        // ── Step 2: Is the timestamp stale? ───────────────────────────────────

        if (age > STALENESS_THRESHOLD_MS) {
            // The Foreground Service has been killed.
            // lastTimestamp == 0L means it never ran (fresh install reboot case).
            android.util.Log.w(
                "SafeSense/Heartbeat",
                "Sensor engine appears dead — last reading ${age / 1000}s ago — firing notification"
            )
            fireRestoreNotification()
        }

        return Result.success()
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun fireRestoreNotification() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the channel if it does not exist yet.
        // createNotificationChannel is idempotent — safe to call every time.
        val channel = NotificationChannel(
            HEARTBEAT_CHANNEL_ID,
            "SafeSense Health Alerts",
            NotificationManager.IMPORTANCE_DEFAULT  // shows in notification drawer, may make sound
        ).apply {
            description = "Alerts you when SafeSense may have been paused by the system"
        }
        notificationManager.createNotificationChannel(channel)

        // When the user taps the notification, restart the Foreground Service.
        // We use an explicit Intent to SensorForegroundService with ACTION_START.
        // FLAG_UPDATE_CURRENT: if a pending intent already exists (user didn't tap
        // the last notification), update it instead of creating a duplicate.
        val restartIntent = Intent(context, SensorForegroundService::class.java).apply {
            action = SensorForegroundService.ACTION_START
        }
        val restartPendingIntent = PendingIntent.getForegroundService(
            context,
            0,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // We also set the notification's tap action to open MainActivity
        // so the user can see the app is running again after restoring.
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, HEARTBEAT_CHANNEL_ID)
            .setContentTitle("SafeSense may have been paused")
            .setContentText("Tap to restore monitoring.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("SafeSense may have been paused by your phone. Tap to restore monitoring and stay protected.")
            )
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)           // dismiss when tapped
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_media_play,
                "Restore",
                restartPendingIntent
            )
            .build()

        notificationManager.notify(HEARTBEAT_NOTIFICATION_ID, notification)
    }
}