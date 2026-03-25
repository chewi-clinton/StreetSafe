package com.example.safesense.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.safesense.data.preferences.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// BootReceiver.kt
// Location: sensor/BootReceiver.kt
//
// PURPOSE:
//   When the device restarts, all running services are killed. Without this
//   receiver, a user who had monitoring active before a reboot would wake up
//   with SafeSense silently not running — the most dangerous failure mode
//   for a safety app.
//
//   BootReceiver listens for BOOT_COMPLETED. When it fires, it checks
//   DataStore for IS_MONITORING. If true (the user had monitoring active
//   before the reboot), it restarts SensorForegroundService automatically.
//
// HOW IT'S TRIGGERED:
//   Android fires android.intent.action.BOOT_COMPLETED after the device
//   finishes booting. Our manifest declares this receiver with that filter.
//   The RECEIVE_BOOT_COMPLETED permission is also declared in the manifest.
//
// WHY @AndroidEntryPoint:
//   We need DataStore injected. Without @AndroidEntryPoint, Hilt cannot
//   inject into a BroadcastReceiver.
//
// WHY a manual CoroutineScope here:
//   BroadcastReceivers have a very short execution window — Android can
//   kill the process right after onReceive() returns. We use goAsync() to
//   tell Android "wait, I'm not done yet" and give ourselves time to read
//   DataStore before starting the service.
// ─────────────────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "BOOT_COMPLETED received — checking if monitoring should restart")

        // goAsync() extends the BroadcastReceiver's execution window.
        // Without it, Android considers onReceive() finished the moment this
        // function returns — before our coroutine has a chance to read DataStore.
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Read the current preferences snapshot.
                // .first() suspends until the first emission — it does not block
                // the main thread because we're on Dispatchers.IO.
                val prefs = dataStore.data.first()
                val isMonitoring = prefs[UserPreferences.IS_MONITORING] ?: false

                Log.d(TAG, "isMonitoring on boot = $isMonitoring")

                if (isMonitoring) {
                    Log.d(TAG, "Restarting SensorForegroundService after reboot")
                    val serviceIntent = Intent(context, SensorForegroundService::class.java).apply {
                        action = SensorForegroundService.ACTION_START
                    }
                    // startForegroundService() is required when starting a
                    // ForegroundService from a background context (which a
                    // BroadcastReceiver is). Using startService() here would
                    // crash on Android 8.0+ (API 26+).
                    context.startForegroundService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading DataStore on boot: ${e.message}")
            } finally {
                // Always call finish() — this releases the wake lock that
                // goAsync() acquired. Forgetting this causes a memory leak.
                pendingResult.finish()
            }
        }
    }
}