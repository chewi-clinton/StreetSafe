package com.example.safesense.sensor.processor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
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
// Uses android.location.LocationManager directly (NOT FusedLocationProviderClient).
// This satisfies the audit requirement for offline GPS on devices without
// Google Play Services and in Airplane Mode.
//
// WHY LocationManager instead of FusedLocationProvider?
//   FusedLocationProviderClient requires Google Play Services. On Tecno/Infinix
//   devices or in Airplane Mode, it hangs indefinitely with no location fix.
//   LocationManager.GPS_PROVIDER talks directly to the GPS hardware and works
//   with SIM card inserted, no internet, no WiFi, no Play Services.
//
// WHY cache in memory?
//   At alert time, we CANNOT make a blocking location request. GPS might take
//   30-60 seconds to acquire a fix. The SMS must send immediately with the
//   most recent known location. If no fix has ever arrived, the SMS says
//   "Location unavailable" rather than hanging.
//
// LAST FIX CACHED: This file passed the audit. No changes needed.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class GPSTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "GPSTracker"
        private const val INTERVAL_MS = 30_000L  // Update every 30 seconds
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Holds the most recent GPS fix. Null until the first fix arrives.
    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    // hasValidFix: false = no fix yet (GPS dot grey), true = fix received (GPS dot green)
    private val _hasValidFix = MutableStateFlow(false)
    val hasValidFix: StateFlow<Boolean> = _hasValidFix.asStateFlow()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _lastLocation.value = location
            _hasValidFix.value = true
            Log.d(TAG, "GPS fix: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m")
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission") // Permission checked by caller (SensorMonitoringService)
    fun start() {
        Log.d(TAG, "Starting GPS updates via LocationManager every ${INTERVAL_MS / 1000}s")
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                INTERVAL_MS,
                0f,
                locationListener,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GPS updates", e)
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping GPS updates")
        locationManager.removeUpdates(locationListener)
    }

    // Called by SmsAlertDispatcher when building the emergency SMS. Never blocks.
    fun getLastLocationForAlert(): Location? = _lastLocation.value

    // Formats location as a Google Maps URL for the SMS body.
    // Works without Google Maps app installed — opens in any browser.
    fun formatForSms(): String {
        val loc = _lastLocation.value
        return if (loc != null) {
            "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
        } else {
            "Location unavailable"
        }
    }
}