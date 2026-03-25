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
//   When it detects a sound louder than DISTRESS_THRESHOLD_DB (85 dB),
//   it emits an AudioEvent.DistressSound on the audioEvents SharedFlow.
//   SensorFusionEngine listens to this and uses it to corroborate falls
//   and collisions.
//
// WHY 85 dB?
//   Normal conversation is ~60 dB. A scream or crash is 85–110 dB.
//   85 dB is loud enough to filter out background noise in Yaoundé traffic
//   while still catching a genuine distress sound.
//   This value can be adjusted in Step 9 if we get false positives.
//
// WHY AudioRecord INSTEAD OF MediaRecorder?
//   MediaRecorder writes to a file. We do not want files — we want raw
//   amplitude readings in real time. AudioRecord gives us raw PCM samples
//   directly in memory, which we convert to dB. No storage required.
//
// WHY NOT run on Dispatchers.Main?
//   Audio processing is CPU-heavy. Running it on Dispatchers.IO keeps it
//   off the main thread so the UI never stutters.
//
// PERMISSION:
//   RECORD_AUDIO permission must be granted before start() is called.
//   The caller (SensorForegroundService) is responsible for checking this.
//   If the permission is missing, AudioRecord initialization will fail
//   and start() returns silently without crashing.
// ─────────────────────────────────────────────────────────────────────────────

class AudioMonitor {

    companion object {
        const val DISTRESS_THRESHOLD_DB = 85.0    // dB — above this = distress sound
        const val SAMPLE_RATE_HZ        = 44100   // standard audio sample rate
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

    // ── Start ─────────────────────────────────────────────────────────────────

    fun start() {
        if (_isActive.value) return  // already running — do not double-start

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        // If bufferSize is ERROR or ERROR_BAD_VALUE, the device cannot record.
        // This should not happen on any real device but we guard against it.
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

        samplingJob = monitorScope.launch {
            val buffer = ShortArray(bufferSize)

            while (isActive) {
                val readCount = record.read(buffer, 0, buffer.size)

                if (readCount > 0) {
                    val db = calculateDecibels(buffer, readCount)

                    val event = when {
                        db >= DISTRESS_THRESHOLD_DB ->
                            AudioEvent.DistressSound(db, System.currentTimeMillis())
                        else ->
                            AudioEvent.Normal(db, System.currentTimeMillis())
                    }

                    _audioEvents.tryEmit(event)
                }
            }
        }

        android.util.Log.d("SafeSense/Audio", "AudioMonitor started")
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