package com.example.safesense.sensor.processor

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log10

// ─────────────────────────────────────────────────────────────────────────────
// AudioMonitor.kt
// Location: sensor/processor/AudioMonitor.kt
//
// WHAT THIS DOES:
//   Listens to the microphone continuously in the background.
//   When it detects a sound louder than 90 dB that SUSTAINS for 2 continuous
//   seconds, it emits an AudioEvent.DistressSound on the audioEvents SharedFlow.
//   SensorFusionEngine listens to this and uses it to corroborate collisions.
//
// WHY 90 dB? (not 85)
//   The build guide specifies 90 dB. Normal conversation is ~60 dB. A scream
//   or crash metal-on-metal impact is 90–110 dB. 90 dB filters out shouting
//   and loud traffic while catching genuine distress.
//
// WHY 2-SECOND SUSTAIN?
//   Without sustain, any momentary noise (door slam, car horn, dropping an
//   object) would emit a DistressSound event. If that momentary noise happens
//   to coincide with a moto-taxi bump (very common in Yaoundé), the
//   FusionEngine would see high-g + loud sound and dispatch a FALSE alert.
//   Requiring 2 seconds of sustained loud sound eliminates this class of
//   false positive. A real scream or crash sustains. A door slam does not.
//
// TRADE-OFF:
//   The DistressSound event fires at the 2-second mark, not at the start of
//   the sound. This means it may miss the FusionEngine's 500ms correlation
//   window with the initial impact. This is ACCEPTABLE because:
//   - Audio is a CORROBORATION signal, not the primary detection signal
//   - The accelerometer still detects the impact independently
//   - Missing audio corroboration means MEDIUM confidence instead of HIGH
//   - MEDIUM confidence STILL triggers the countdown — the user is still protected
//   - Preventing false alerts is more important than upgrading confidence
//
// WHY AudioRecord INSTEAD OF MediaRecorder?
//   MediaRecorder writes to a file. We do not want files — we want raw
//   amplitude readings in real time. AudioRecord gives us raw PCM samples
//   directly in memory, which we convert to dB. No storage required.
//
// WHY System.currentTimeMillis() HERE instead of sensor timestamp?
//   Unlike accelerometer/proximity sensors, AudioRecord does not provide
//   hardware clock timestamps. System.currentTimeMillis() is the only
//   option. This is acceptable because audio events are used for
//   corroboration, not precise timing correlation.
//
// PERMISSION:
//   RECORD_AUDIO permission must be granted before start() is called.
//   The caller (SensorMonitoringService) is responsible for checking this.
//   If the permission is missing, AudioRecord initialization will fail
//   and start() returns silently without crashing.
// ─────────────────────────────────────────────────────────────────────────────

class AudioMonitor {

    companion object {
        // ── FIX: Changed from 85dB to 90dB as specified in the build guide ──
        const val DISTRESS_THRESHOLD_DB = 90.0       // dB — above this = potential distress sound
        const val SUSTAIN_REQUIRED_MS   = 2000L      // Must sustain for 2 continuous seconds
        const val SAMPLE_RATE_HZ        = 44100      // Standard audio sample rate
        const val CHANNEL_CONFIG        = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT          = AudioFormat.ENCODING_PCM_16BIT
    }

    private val _audioEvents = MutableSharedFlow<AudioEvent>(extraBufferCapacity = 32)
    val audioEvents: SharedFlow<AudioEvent> = _audioEvents.asSharedFlow()

    // isActive: true when microphone is open and sampling
    // Observed by HomeViewModel to drive the audio dot on HomeScreen
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var samplingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    // ── Sustain tracking state ────────────────────────────────────────────────
    // These track whether a loud sound has been continuous for 2 seconds.
    // They are only accessed from the single coroutine in samplingJob,
    // so no synchronization is needed.

    // The time (System.currentTimeMillis) when the sound first went above threshold.
    // null means no loud sound is currently being tracked.
    private var distressStartTime: Long? = null

    // True if we already emitted a DistressSound for the current sustained event.
    // Prevents emitting multiple DistressSound events for one long scream.
    private var hasEmittedForCurrentSustain: Boolean = false

    // ── Start ─────────────────────────────────────────────────────────────────

    fun start() {
        if (_isActive.value) return  // already running — do not double-start

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        // If bufferSize is ERROR or ERROR_BAD_VALUE, the device cannot record.
        if (bufferSize <= 0) {
            android.util.Log.w("SafeSense/Audio", "AudioRecord buffer size invalid: $bufferSize")
            return
        }

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_HZ,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (e: SecurityException) {
            // RECORD_AUDIO permission not granted — fail silently
            android.util.Log.w("SafeSense/Audio", "RECORD_AUDIO permission missing: ${e.message}")
            return
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            android.util.Log.w("SafeSense/Audio", "AudioRecord failed to initialize")
            record.release()
            return
        }

        audioRecord = record
        record.startRecording()
        _isActive.value = true

        // ── Reset sustain state on every start ──
        distressStartTime = null
        hasEmittedForCurrentSustain = false

        samplingJob = monitorScope.launch {
            val buffer = ShortArray(bufferSize)

            while (isActive) {
                val readCount = record.read(buffer, 0, buffer.size)

                if (readCount > 0) {
                    val db = calculateDecibels(buffer, readCount)
                    val now = System.currentTimeMillis()

                    // ── FIX #4: 2-second sustain check ───────────────────────
                    if (db >= DISTRESS_THRESHOLD_DB) {
                        // Sound is loud — start or continue tracking
                        if (distressStartTime == null) {
                            // First buffer above threshold — start the timer
                            distressStartTime = now
                            hasEmittedForCurrentSustain = false
                            android.util.Log.d(
                                "SafeSense/Audio",
                                "Loud sound detected (${String.format("%.1f", db)} dB) — starting 2s sustain timer"
                            )
                        }

                        // Check if sound has sustained for 2 seconds
                        // AND we haven't already emitted for this event
                        if (!hasEmittedForCurrentSustain) {
                            val sustainedMs = now - (distressStartTime ?: now)
                            if (sustainedMs >= SUSTAIN_REQUIRED_MS) {
                                hasEmittedForCurrentSustain = true
                                _audioEvents.tryEmit(
                                    AudioEvent.DistressSound(db, now)
                                )
                                android.util.Log.w(
                                    "SafeSense/Audio",
                                    "DISTRESS SOUND CONFIRMED after ${sustainedMs}ms at ${String.format("%.1f", db)} dB"
                                )
                            }
                        }
                        // While above threshold but before 2s: emit nothing.
                        // This silence prevents the FusionEngine from seeing
                        // a premature DistressSound that would cause a false alert.

                    } else {
                        // Sound dropped below threshold — reset everything
                        if (distressStartTime != null) {
                            val sustainedMs = now - (distressStartTime ?: now)
                            android.util.Log.d(
                                "SafeSense/Audio",
                                "Sound dropped below threshold after ${sustainedMs}ms — sustain check failed, reset"
                            )
                        }
                        distressStartTime = null
                        hasEmittedForCurrentSustain = false

                        // Emit Normal to show the mic is alive
                        _audioEvents.tryEmit(AudioEvent.Normal(db, now))
                    }
                }
            }
        }

        android.util.Log.d("SafeSense/Audio", "AudioMonitor started (threshold=90dB, sustain=2s)")
    }

    // ── Stop ──────────────────────────────────────────────────────────────────

    fun stop() {
        samplingJob?.cancel()
        samplingJob = null

        audioRecord?.let { record ->
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                record.stop()
            }
            record.release()
        }
        audioRecord = null
        _isActive.value = false

        // ── Reset sustain state on stop ──
        distressStartTime = null
        hasEmittedForCurrentSustain = false

        android.util.Log.d("SafeSense/Audio", "AudioMonitor stopped")
    }

    // ── dB calculation ────────────────────────────────────────────────────────
    //
    // We take the average absolute amplitude of all PCM samples in the buffer,
    // then convert to decibels using the standard formula: 20 * log10(amplitude).
    // PCM 16-bit max value is 32767, so we normalise against that.
    //
    // Why average instead of peak?
    // Peak amplitude spikes on single samples — noise, clicks, codec artefacts.
    // Average amplitude over a buffer (typically 1000–4000 samples) gives a
    // stable reading that reflects actual loudness over a brief window.

    private fun calculateDecibels(buffer: ShortArray, readCount: Int): Double {
        var sum = 0.0
        for (i in 0 until readCount) {
            sum += abs(buffer[i].toInt())
        }
        val average = sum / readCount

        // Guard against log10(0) which is -infinity
        if (average < 1.0) return 0.0

        return 20.0 * log10(average / 32767.0 * 100)
    }
}

// ── Audio events ──────────────────────────────────────────────────────────────

sealed class AudioEvent {
    data class Normal(val decibels: Double, val timestamp: Long)       : AudioEvent()
    data class DistressSound(val decibels: Double, val timestamp: Long) : AudioEvent()
}