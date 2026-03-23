package com.example.safesense.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SensorForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "safesense_sensor_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSensorEngine()
            ACTION_STOP -> stopSensorEngine()
        }
        return START_STICKY
    }

    private fun startSensorEngine() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        // Sensor processors will be initialized here in Step 3
    }

    private fun stopSensorEngine() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeSense is active")
            .setContentText("Monitoring your safety")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SafeSense Sensor Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps SafeSense running in the background"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}