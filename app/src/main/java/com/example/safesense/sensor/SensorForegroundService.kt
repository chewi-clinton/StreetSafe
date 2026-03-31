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
import com.example.safesense.domain.model.DetectedIncident
import com.example.safesense.domain.usecase.RecognizeShakeGestureUseCase
import com.example.safesense.sensor.fusion.SensorFusionEngine
import com.example.safesense.sensor.processor.AccelerometerProcessor
import com.example.safesense.sensor.processor.AudioEvent
import com.example.safesense.sensor.processor.AudioMonitor
import com.example.safesense.sensor.processor.GPSTracker
import com.example.safesense.sensor.processor.ProximityProcessor
import com.example.safesense.sensor.worker.SensorHeartbeatWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

        // ── Live status — observed by HomeViewModel ───────────────────────────
        private val _accelerometerActive = MutableStateFlow(false)
        val accelerometerActive: StateFlow<Boolean> = _accelerometerActive.asStateFlow()

        private val _audioActive = MutableStateFlow(false)
        val audioActive: StateFlow<Boolean> = _audioActive.asStateFlow()

        // ── Incident stream — observed by CountdownViewModel ──────────────────
        private val _incidents = MutableSharedFlow<DetectedIncident>(
            extraBufferCapacity = 1
        )
        val incidents: SharedFlow<DetectedIncident> = _incidents.asSharedFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometerProcessor: AccelerometerProcessor
    private lateinit var proximityProcessor: ProximityProcessor
    private lateinit var audioMonitor: AudioMonitor

    @Inject lateinit var fusionEngine: SensorFusionEngine
    @Inject lateinit var gpsTracker: GPSTracker
    @Inject lateinit var dataStore: DataStore<Preferences>
    @Inject lateinit var recognizeShakeGestureUseCase: RecognizeShakeGestureUseCase

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorManager          = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometerProcessor = AccelerometerProcessor(sensorManager)
        proximityProcessor     = ProximityProcessor(sensorManager)
        audioMonitor           = AudioMonitor()
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
        audioMonitor.stop()
        _accelerometerActive.value = false
        _audioActive.value = false
        serviceScope.cancel()
    }

    // ── Engine start/stop ─────────────────────────────────────────────────────

    private fun startSensorEngine() {
        startForeground(NOTIFICATION_ID, buildNotification())

        accelerometerProcessor.start()
        proximityProcessor.start()
        gpsTracker.start()

        // AUDIT FIX: Load user-configured shake thresholds before processing starts
        serviceScope.launch {
            recognizeShakeGestureUseCase.loadPreferences()
        }

        // Only start audio monitor if enabled in settings
        serviceScope.launch {
            val prefs = dataStore.data.first()
            val audioEnabled = prefs[UserPreferences.MICROPHONE_DETECTION_ENABLED] ?: false
            if (audioEnabled) {
                audioMonitor.start()
            }
        }

        // Pipe status to companion objects
        accelerometerProcessor.isActive
            .onEach { isActive -> _accelerometerActive.value = isActive }
            .launchIn(serviceScope)

        audioMonitor.isActive
            .onEach { isActive -> _audioActive.value = isActive }
            .launchIn(serviceScope)

        collectAccelerometerEvents()
        collectProximityEvents()
        collectAudioEvents()
        collectShakeEvents()
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
        audioMonitor.stop()
        _accelerometerActive.value = false
        _audioActive.value = false

        cancelHeartbeat()

        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Event collectors ──────────────────────────────────────────────────────

    private fun collectAccelerometerEvents() {
        serviceScope.launch {
            accelerometerProcessor.accelerometerEvents.collect { event ->
                fusionEngine.onAccelerometerEvent(event)
                dataStore.edit { prefs ->
                    prefs[UserPreferences.LAST_ACCELEROMETER_TIMESTAMP] = event.timestamp
                }
            }
        }
    }

    private fun collectProximityEvents() {
        serviceScope.launch {
            proximityProcessor.proximityEvents.collect { event ->
                fusionEngine.onProximityChanged(
                    timestamp = event.timestamp,
                    isNear    = event.state == com.example.safesense.sensor.processor.ProximityState.NEAR
                )
            }
        }
    }

    private fun collectAudioEvents() {
        serviceScope.launch {
            audioMonitor.audioEvents.collect { event ->
                if (event is AudioEvent.DistressSound) {
                    fusionEngine.onDistressSoundDetected(event.timestamp)
                }
            }
        }
    }

    private fun collectShakeEvents() {
        serviceScope.launch {
            accelerometerProcessor.accelerometerEvents.collect { event ->
                val result = recognizeShakeGestureUseCase.process(event)
                if (result is com.example.safesense.domain.model.DetectionResult.Detected) {
                    fusionEngine.onShakeEvent(event.timestamp)
                }
            }
        }
    }

    private fun collectIncidents() {
        serviceScope.launch {
            fusionEngine.incidents.collect { incident ->
                _incidents.emit(incident)
            }
        }
    }

    // ── WorkManager heartbeat ─────────────────────────────────────────────────

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

    private fun cancelHeartbeat() {
        WorkManager.getInstance(this).cancelUniqueWork(SensorHeartbeatWorker.WORK_NAME)
    }

    // ── Notification ──────────────────────────────────────────────────────────

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
