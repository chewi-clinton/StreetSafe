package com.example.safesense.sensor.worker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SensorHeartbeatWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "safesense_sensor_heartbeat"
        const val HEARTBEAT_CHANNEL_ID = "safesense_heartbeat_channel"
        const val HEARTBEAT_NOTIFICATION_ID = 2
    }

    override suspend fun doWork(): Result {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        return if (accelerometer == null) {
            fireAlertNotification()
            Result.failure()
        } else {
            Result.success()
        }
    }

    private fun fireAlertNotification() {
        val notification = NotificationCompat.Builder(context, HEARTBEAT_CHANNEL_ID)
            .setContentTitle("SafeSense Warning")
            .setContentText("Accelerometer not responding. Your safety monitoring may be affected.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(HEARTBEAT_NOTIFICATION_ID, notification)
        }
    }
}