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

@Singleton
class GPSTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "GPSTracker"
        private const val INTERVAL_MS = 10_000L  // Update every 10 seconds for better responsiveness
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    private val _hasValidFix = MutableStateFlow(false)
    val hasValidFix: StateFlow<Boolean> = _hasValidFix.asStateFlow()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _lastLocation.value = location
            _hasValidFix.value = true
            Log.d(TAG, "Location update: lat=${location.latitude}, lng=${location.longitude}, source=${location.provider}")
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun start() {
        Log.d(TAG, "Starting location updates")
        try {
            // Request from GPS_PROVIDER for high accuracy
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    INTERVAL_MS,
                    0f,
                    locationListener,
                    Looper.getMainLooper()
                )
            }

            // Also request from NETWORK_PROVIDER for faster indoor fix
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    INTERVAL_MS,
                    0f,
                    locationListener,
                    Looper.getMainLooper()
                )
            }
            
            // Try to get last known location immediately
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLast = if (isBetterLocation(lastGps, lastNet)) lastGps else lastNet
            
            if (bestLast != null) {
                _lastLocation.value = bestLast
                _hasValidFix.value = true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates", e)
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping location updates")
        locationManager.removeUpdates(locationListener)
    }

    private fun isBetterLocation(loc1: Location?, loc2: Location?): Boolean {
        if (loc1 == null) return false
        if (loc2 == null) return true
        return loc1.time > loc2.time || loc1.accuracy < loc2.accuracy
    }

    fun getLastLocationForAlert(): Location? = _lastLocation.value

    fun formatForSms(): String {
        val loc = _lastLocation.value
        return if (loc != null) {
            "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
        } else {
            "Location unavailable"
        }
    }
}
