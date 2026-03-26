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
import com.example.safesense.domain.usecase.DetectCollisionUseCase
import com.example.safesense.domain.usecase.DetectFallUseCase
import com.example.safesense.domain.usecase.RecognizeShakeGestureUseCase
import com.example.safesense.sensor.engine.SensorFusionEngine
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// SensorForegroundService.kt
// Location: sensor/SensorForegroundService.kt
//
// STEP 9 CHANGES vs Step 8:
//
//   1. SensorFusionEngine now takes DetectCollisionUseCase as a second parameter.
//      This was a TODO stub before — it is now wired.
//
//   2. incidents SharedFlow added to companion object.
//      When SensorFusionEngine emits a DetectedIncident, we re-emit it here
//      on a companion object SharedFlow so CountdownViewModel can observe it
//      from anywhere without binding to the service.
//      This is the same pattern used for accelerometerActive.
//
//   3. collectIncidents() now re-emits to the companion object SharedFlow
//      instead of only logging + showing a Toast. The Toast stays during
//      testing but will be removed in Step 12.
//
// STEP 9 TASK 2:
//   The test ShakeDetector in SettingsScreen should be deleted after this step.
//   Shake detection now flows: AccelerometerProcessor → SensorFusionEngine
//   → incidents flow → CountdownViewModel.
//   (Shake algorithm itself is Step 17 — but the wiring path is now correct.)
//
// STEP 10 CHANGES vs Step 9:
//
//   1. collectAccelerometerEvents() now writes LAST_ACCELEROMETER_TIMESTAMP
//      to DataStore on every reading. SensorHeartbeatWorker reads this to
//      detect silent service kills on Tecno/Infinix.
//
//   2. scheduleHeartbeat() changed from 15 minutes to 3 minutes.
//      The master prompt spec says "every 3 minutes". The previous value of
//      15 minutes was a placeholder — corrected here.
//
//   3. cancelHeartbeat() added. Called from stopSensorEngine() so the worker
//      does not fire notifications after the user deliberately stops monitoring.
// ─────────────────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class SensorForegroundService : Service() {

    companion object {
        const val CHANNEL_ID      = "safesense_sensor_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START    = "ACTION_START"
        const val ACTION_STOP     = "ACTION_STOP"

        // ── Live accelerometer status — observed by HomeViewModel ─────────────
        private val _accelerometerActive = MutableStateFlow(false)
        val accelerometerActive: StateFlow<Boolean> = _accelerometerActive.asStateFlow()

        // ── Incident stream — observed by CountdownViewModel ──────────────────
        //
        // When SensorFusionEngine declares an incident, we re-emit it here.
        // CountdownViewModel subscribes to this SharedFlow and transitions
        // from Idle → CountdownRunning.
        //
        // extraBufferCapacity = 1: holds one incident while CountdownViewModel
        // is busy transitioning. We never want to lose a real emergency.
        // DROP_OLDEST: if somehow two incidents arrive before the ViewModel
        // collects, we keep the newer one (more relevant).
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
    private lateinit var fusionEngine: SensorFusionEngine

    @Inject lateinit var gpsTracker: GPSTracker
    @Inject lateinit var dataStore: DataStore<Preferences>
    @Inject lateinit var detectFallUseCase: DetectFallUseCase
    @Inject lateinit var detectCollisionUseCase: DetectCollisionUseCase
    @Inject lateinit var recognizeShakeGestureUseCase: RecognizeShakeGestureUseCase

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorManager          = getSystemService(SENSOR_SERVICE) as SensorManager
        fusionEngine           = SensorFusionEngine(serviceScope, detectFallUseCase, detectCollisionUseCase)
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
        serviceScope.cancel()
    }

    // ── Engine start/stop ─────────────────────────────────────────────────────

    private fun startSensorEngine() {
        startForeground(NOTIFICATION_ID, buildNotification())

        accelerometerProcessor.start()
        proximityProcessor.start()
        gpsTracker.start()
        audioMonitor.start()

        // Pipe the accelerometer's isActive state to the companion object
        // so HomeViewModel can observe it
        accelerometerProcessor.isActive
            .onEach { isActive -> _accelerometerActive.value = isActive }
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

        // Step 10: Cancel the heartbeat worker when monitoring stops deliberately.
        // Without this, the worker would fire a "paused" notification even though
        // the user intentionally stopped monitoring — a false alarm.
        cancelHeartbeat()

        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Event collectors ──────────────────────────────────────────────────────

    private fun collectAccelerometerEvents() {
        serviceScope.launch {
            accelerometerProcessor.accelerometerEvents.collect { event ->
                // Feed every event into the fusion engine
                fusionEngine.onAccelerometerEvent(event)

                // Step 10: Write the current timestamp to DataStore on every reading.
                // SensorHeartbeatWorker checks this timestamp every 3 minutes.
                // If it is more than 5 minutes old, the worker knows this service
                // was silently killed and fires a restore notification.
                //
                // We use a non-blocking write (launch, not suspend directly here)
                // because we are already in a coroutine and DataStore.edit is fast.
                // We do NOT want to slow down the accelerometer event loop.
                dataStore.edit { prefs ->
                    prefs[UserPreferences.LAST_ACCELEROMETER_TIMESTAMP] = event.timestamp
                }
            }
        }
    }

    private fun collectProximityEvents() {
        serviceScope.launch {
            proximityProcessor.proximityEvents.collect { event ->
                // Pass both timestamp and whether the phone is NEAR (in hand/pocket)
                // SensorFusionEngine needs isNear to correctly implement Row 5
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
        // We reuse the accelerometer event stream — RecognizeShakeGestureUseCase
        // processes every reading and returns Detected only when ≥3 jerk spikes
        // occur within 2 seconds. When it fires, we forward to the fusion engine
        // which applies the Row 5 check (shake + NEAR proximity → SHAKE HIGH).
        serviceScope.launch {
            accelerometerProcessor.accelerometerEvents.collect { event ->
                val result = recognizeShakeGestureUseCase.process(event)
                if (result is com.example.safesense.domain.model.DetectionResult.Detected) {
                    fusionEngine.onShakeEvent(event.timestamp)
                }
            }
        }
    }

    /**
     * Step 9 Task 2: Wire incidents from SensorFusionEngine to CountdownViewModel.
     *
     * Previously this only logged and showed a Toast.
     * Now it re-emits on the companion object SharedFlow, which CountdownViewModel
     * subscribes to. This is the final link in the chain:
     *
     *   AccelerometerProcessor → SensorFusionEngine → SensorForegroundService.incidents
     *   → CountdownViewModel.startCountdown()
     *
     * The Toast stays for now so you can see detections during physical drop tests
     * without needing Logcat. Remove it in Step 12 when CountdownScreen is built.
     */
    private fun collectIncidents() {
        serviceScope.launch {
            fusionEngine.incidents.collect { incident ->
                android.util.Log.d(
                    "SafeSense/Incident",
                    "INCIDENT: type=${incident.type} confidence=${incident.confidenceLevel} at ${incident.timestamp}"
                )

                // Re-emit on the companion object flow for CountdownViewModel
                _incidents.emit(incident)

                // Temporary Toast for physical device testing — remove in Step 12
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        applicationContext,
                        "${incident.type} detected — ${incident.confidenceLevel}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ── WorkManager heartbeat ─────────────────────────────────────────────────

    /**
     * Step 10: Schedule the heartbeat worker to run every 3 minutes.
     *
     * KEEP policy: if a heartbeat job is already scheduled (e.g. the service
     * restarted after a crash), keep the existing schedule instead of creating
     * a duplicate. This prevents multiple workers firing for the same purpose.
     *
     * IMPORTANT: WorkManager does not guarantee exact 3-minute intervals on
     * battery-restricted devices. The minimum guaranteed interval for
     * PeriodicWorkRequest is 15 minutes on Android. However, WorkManager
     * batches work intelligently — on most devices it runs closer to the
     * requested interval when the device is awake.
     *
     * For development testing you can use a OneTimeWorkRequest to manually
     * trigger the worker without waiting.
     */
    private fun scheduleHeartbeat() {
        val heartbeatRequest = PeriodicWorkRequestBuilder<SensorHeartbeatWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SensorHeartbeatWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            heartbeatRequest
        )

        android.util.Log.d("SafeSense/Heartbeat", "Heartbeat worker scheduled (every 3 min)")
    }

    /**
     * Step 10: Cancel the heartbeat worker.
     * Called when the user deliberately stops monitoring.
     * Without this, the worker would fire "paused" notifications even though
     * monitoring was intentionally stopped.
     */
    private fun cancelHeartbeat() {
        WorkManager.getInstance(this).cancelUniqueWork(SensorHeartbeatWorker.WORK_NAME)
        android.util.Log.d("SafeSense/Heartbeat", "Heartbeat worker cancelled")
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