package com.example.safesense.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.example.safesense.MainActivity
import com.example.safesense.data.preferences.UserPreferences
import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.DetectedIncident
import com.example.safesense.domain.model.DetectionResult
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.domain.usecase.RecognizeShakeGestureUseCase
import com.example.safesense.sensor.fusion.SensorFusionEngine
import com.example.safesense.sensor.processor.AccelerometerProcessor
import com.example.safesense.sensor.processor.AudioEvent
import com.example.safesense.sensor.processor.AudioMonitor
import com.example.safesense.sensor.processor.GPSTracker
import com.example.safesense.sensor.processor.ProximityProcessor
import com.example.safesense.sensor.processor.ProximityState
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class SensorForegroundService : Service() {

    companion object {
        const val CHANNEL_ID      = "safesense_sensor_channel"
        const val ALERT_CHANNEL_ID = "safesense_alert_channel"
        const val NOTIFICATION_ID = 1
        const val ALERT_NOTIFICATION_ID = 2
        const val ACTION_START    = "ACTION_START"
        const val ACTION_STOP     = "ACTION_STOP"

        private val _accelerometerActive = MutableStateFlow(false)
        val accelerometerActive: StateFlow<Boolean> = _accelerometerActive.asStateFlow()

        private val _proximityActive = MutableStateFlow(false)
        val proximityActive: StateFlow<Boolean> = _proximityActive.asStateFlow()

        private val _audioActive = MutableStateFlow(false)
        val audioActive: StateFlow<Boolean> = _audioActive.asStateFlow()

        private val _incidents = MutableSharedFlow<DetectedIncident>(
            replay = 0,
            extraBufferCapacity = 64 
        )
        val incidents: SharedFlow<DetectedIncident> = _incidents.asSharedFlow()
    }

    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometerProcessor: AccelerometerProcessor
    private lateinit var proximityProcessor: ProximityProcessor
    private lateinit var audioMonitor: AudioMonitor

    @Inject lateinit var fusionEngine: SensorFusionEngine
    @Inject lateinit var gpsTracker: GPSTracker
    @Inject lateinit var dataStore: DataStore<Preferences>
    @Inject lateinit var recognizeShakeGestureUseCase: RecognizeShakeGestureUseCase

    private var isEngineRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
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
        stopSensorEngine()
    }

    private fun startSensorEngine() {
        if (isEngineRunning) return
        isEngineRunning = true

        // Re-create scope if it was cancelled
        if (!serviceScope.launch { }.isActive) {
            serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }

        startForeground(NOTIFICATION_ID, buildStatusNotification())

        accelerometerProcessor.start()
        proximityProcessor.start()
        gpsTracker.start()
        
        _proximityActive.value = true

        // Observe microphone setting in real-time
        observeAudioSetting()

        // Sync processor status flows to our companion state flows
        accelerometerProcessor.isActive
            .onEach { isActive -> _accelerometerActive.value = isActive }
            .launchIn(serviceScope)

        audioMonitor.isActive
            .onEach { isActive -> _audioActive.value = isActive }
            .launchIn(serviceScope)

        // Launch shake preference loading and event collection
        serviceScope.launch {
            recognizeShakeGestureUseCase.loadPreferences()
            collectShakeEvents()
        }

        collectAccelerometerEvents()
        collectProximityEvents()
        collectAudioEvents()
        collectIncidents()
        scheduleHeartbeat()

        serviceScope.launch {
            dataStore.edit { prefs ->
                prefs[UserPreferences.IS_MONITORING] = true
            }
        }
    }

    private fun stopSensorEngine() {
        if (!isEngineRunning) return
        isEngineRunning = false

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
        _proximityActive.value     = false
        _audioActive.value         = false

        cancelHeartbeat()

        serviceScope.cancel()
        // Re-initialize for next use
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun collectAccelerometerEvents() {
        serviceScope.launch {
            accelerometerProcessor.accelerometerEvents.collect { event ->
                fusionEngine.onAccelerometerEvent(event)
                dataStore.edit { prefs ->
                    prefs[UserPreferences.LAST_ACCELEROMETER_TIMESTAMP] = System.currentTimeMillis()
                }
            }
        }
    }

    private fun collectProximityEvents() {
        serviceScope.launch {
            proximityProcessor.proximityEvents.collect { event ->
                fusionEngine.onProximityChanged(
                    timestamp = event.timestamp,
                    isNear    = event.state == ProximityState.NEAR
                )
            }
        }
    }

    private fun observeAudioSetting() {
        dataStore.data
            .map { prefs -> prefs[UserPreferences.MICROPHONE_DETECTION_ENABLED] ?: false }
            .distinctUntilChanged()
            .onEach { enabled ->
                if (enabled) {
                    android.util.Log.d("SensorForegroundService", "Microphone enabled in settings. Starting monitor.")
                    audioMonitor.start()
                } else {
                    android.util.Log.d("SensorForegroundService", "Microphone disabled in settings. Stopping monitor.")
                    audioMonitor.stop()
                }
            }
            .launchIn(serviceScope)
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
                if (result is DetectionResult.Detected) {
                    android.util.Log.d("SensorForegroundService", "Shake detected! Forwarding to FusionEngine.")
                    fusionEngine.onShakeEvent(event.timestamp)
                }
            }
        }
    }

    private fun collectIncidents() {
        serviceScope.launch {
            fusionEngine.incidents.collect { incident ->
                android.util.Log.d("SensorForegroundService", "Incident received from FusionEngine: ${incident.type}")
                
                // Always emit to the flow for UI listeners
                _incidents.tryEmit(incident)
                
                // If the UI is not in the foreground (no active subscribers), show a high-priority notification
                if (_incidents.subscriptionCount.value == 0) {
                    showEmergencyNotification(incident)
                }
            }
        }
    }

    private fun showEmergencyNotification(incident: DetectedIncident) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("incident_type", incident.type.name)
            putExtra("confidence", incident.confidenceLevel.name)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (incident.type) {
            IncidentType.FALL -> "Fall Detected!"
            IncidentType.COLLISION -> "Collision Detected!"
            IncidentType.SHAKE -> "Emergency Triggered!"
            else -> "SafeSense Alert!"
        }

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Tap to open SafeSense and cancel if this is a false alarm.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // This makes it "pop up"
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(ALERT_NOTIFICATION_ID, notification)
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

    private fun cancelHeartbeat() {
        WorkManager.getInstance(this).cancelUniqueWork(SensorHeartbeatWorker.WORK_NAME)
    }

    private fun buildStatusNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeSense is active")
            .setContentText("Monitoring your safety")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        // Status channel
        val statusChannel = NotificationChannel(
            CHANNEL_ID,
            "SafeSense Status",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(statusChannel)

        // Alert channel (High priority)
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "SafeSense Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority alerts for emergencies"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(alertChannel)
    }
}
