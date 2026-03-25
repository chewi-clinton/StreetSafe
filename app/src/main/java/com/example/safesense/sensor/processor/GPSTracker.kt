package com.example.safesense.sensor.processor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// GPSTracker.kt
// Location: sensor/processor/GPSTracker.kt
//
// BLOCK 4 CHANGES:
//   - Added hasValidFix: StateFlow<Boolean>
//     Starts false. Flips to true the moment the first GPS fix arrives.
//     HomeViewModel observes this to turn the GPS dot green in real time.
//   - Removed formatForDebug() — Block 3 test is done, no longer needed.
//
// EVERYTHING ELSE IS UNCHANGED from Block 3.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class GPSTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "GPSTracker"
        private const val INTERVAL_MS = 30_000L
        private const val FASTEST_INTERVAL_MS = 15_000L
    }

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Holds the most recent GPS fix. Null until the first fix arrives.
    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    // ── BLOCK 4: GPS fix availability flag ───────────────────────────────────
    // false = no fix yet  →  GPS dot stays grey
    // true  = fix received →  GPS dot turns green
    //
    // HomeViewModel observes this. When it flips to true, it sets gpsActive=true
    // in HomeUiState. The GPS pill on HomeScreen reacts via animateColorAsState.
    private val _hasValidFix = MutableStateFlow(false)
    val hasValidFix: StateFlow<Boolean> = _hasValidFix.asStateFlow()

    private val locationRequest: LocationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        INTERVAL_MS
    )
        .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
        .setGranularity(com.google.android.gms.location.Granularity.GRANULARITY_FINE)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            _lastLocation.value = location
            _hasValidFix.value = true   // ← this is what turns the dot green
            Log.d(TAG, "GPS fix: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m")
        }
    }

    @SuppressLint("MissingPermission") // Permission checked by caller (SensorMonitoringService)
    fun start() {
        Log.d(TAG, "Starting GPS updates every ${INTERVAL_MS / 1000}s")
        fusedClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stop() {
        Log.d(TAG, "Stopping GPS updates")
        fusedClient.removeLocationUpdates(locationCallback)
        // We do NOT reset _hasValidFix here — the cached location is still valid.
    }

    // Called by SmsAlertDispatcher when building the emergency SMS. Never blocks.
    fun getLastLocationForAlert(): Location? = _lastLocation.value

    // Formats location as a Google Maps URL for the SMS body.
    // Opens in any browser — no Google Maps app required.
    fun formatForSms(): String {
        val loc = _lastLocation.value
        return if (loc != null) {
            "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
        } else {
            "Location unavailable"
        }
    }
}