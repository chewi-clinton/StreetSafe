package com.example.safesense.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.safesense.data.preferences.UserPreferences
import com.example.safesense.domain.usecase.DetectFallUseCase
import com.example.safesense.sensor.engine.SensorFusionEngine
import com.example.safesense.sensor.processor.AccelerometerProcessor
import com.example.safesense.sensor.processor.GPSTracker
import com.example.safesense.sensor.processor.ProximityProcessor
import com.example.safesense.sensor.worker.SensorHeartbeatWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class SensorForegroundService : Service() {

    companion object {
        const val CHANNEL_ID      = "safesense_sensor_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START    = "ACTION_START"
        const val ACTION_STOP     = "ACTION_STOP"

        private val _accelerometerActive = MutableStateFlow(false)
        val accelerometerActive: StateFlow<Boolean> = _accelerometerActive.asStateFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometerProcessor: AccelerometerProcessor
    private lateinit var proximityProcessor: ProximityProcessor
    private lateinit var fusionEngine: SensorFusionEngine

    @Inject lateinit var gpsTracker: GPSTracker
    @Inject lateinit var dataStore: DataStore<Preferences>
    @Inject lateinit var detectFallUseCase: DetectFallUseCase

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorManager         = getSystemService(SENSOR_SERVICE) as SensorManager
        fusionEngine          = SensorFusionEngine(serviceScope, detectFallUseCase)
        accelerometerProcessor = AccelerometerProcessor(sensorManager)
        proximityProcessor    = ProximityProcessor(sensorManager)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSensorEngine()
            ACTION_STOP  -> stopSensorEngine()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        accelerometerProcessor.stop()
        proximityProcessor.stop()
        gpsTracker.stop()
        _accelerometerActive.value = false
        serviceScope.cancel()
    }

    private fun startSensorEngine() {
        startForeground(NOTIFICATION_ID, buildNotification())
        accelerometerProcessor.start()
        proximityProcessor.start()
        gpsTracker.start()

        accelerometerProcessor.isActive
            .onEach { isActive -> _accelerometerActive.value = isActive }
            .launchIn(serviceScope)

        collectAccelerometerEvents()
        collectProximityEvents()
        collectIncidents()
        scheduleHeartbeat()

        serviceScope.launch {
            dataStore.edit { prefs ->
                prefs[UserPreferences.IS_MONITORING] = true
            }
        }
    }

    private fun stopSensorEngine() {
        serviceScope.launch {
            dataStore.edit { prefs ->
                prefs[UserPreferences.IS_MONITORING] = false
            }
        }

        accelerometerProcessor.stop()
        proximityProcessor.stop()
        gpsTracker.stop()
        _accelerometerActive.value = false
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun collectAccelerometerEvents() {
        serviceScope.launch {
            accelerometerProcessor.accelerometerEvents.collect { event ->
                fusionEngine.onAccelerometerEvent(event)
            }
        }
    }

    private fun collectProximityEvents() {
        serviceScope.launch {
            proximityProcessor.proximityEvents.collect { event ->
                fusionEngine.onProximityChanged(event.timestamp)
            }
        }
    }

    private fun collectIncidents() {
        serviceScope.launch {
            fusionEngine.incidents.collect { incident ->
                android.util.Log.d(
                    "SafeSense/Incident",
                    "FALL DETECTED type=${incident.type} confidence=${incident.confidenceLevel} at ${incident.timestamp}"
                )
                // Temporary toast so you can see it without Logcat during drop test
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        applicationContext,
                        "Fall detected — ${incident.confidenceLevel}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun scheduleHeartbeat() {
        val heartbeatRequest = PeriodicWorkRequestBuilder<SensorHeartbeatWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SensorHeartbeatWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            heartbeatRequest
        )
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
}
